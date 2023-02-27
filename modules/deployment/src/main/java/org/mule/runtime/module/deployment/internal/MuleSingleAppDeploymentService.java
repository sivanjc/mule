/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;

import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleSingleAppDeploymentService implements DeploymentService {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());
  // fair lock
  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);
  private final LazyValue<Scheduler> artifactStartExecutor;

  private Domain defaultDomain;
  private Application application;
  private final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();

  private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
  private final SingleAppApplicationDeployer applicationDeployer;
  private final SingleAppDefaultDomainDeployer domainDeployer;

  public MuleSingleAppDeploymentService(DefaultDomainFactory domainFactory, DefaultApplicationFactory applicationFactory,
                                        Supplier<SchedulerService> artifactStartExecutorSupplier) {
    artifactStartExecutor = new LazyValue<>(() -> artifactStartExecutorSupplier.get()
        .customScheduler(config()
            .withName("ArtifactDeployer.start")
            .withMaxConcurrentTasks(1),
                         1));
    ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>(artifactStartExecutor);
    ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<>(artifactStartExecutor);

    this.applicationDeployer = new SingleAppApplicationDeployer(applicationMuleDeployer,
                                                                applicationFactory,
                                                                this::setApplication,
                                                                new DeploymentMuleContextListenerFactory(applicationDeploymentListener));
    this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);

    this.domainDeployer = new SingleAppDefaultDomainDeployer(domainMuleDeployer, domainFactory, this::setDefaultDomain,
                                                             // TODO really?
                                                             new DomainDeploymentTemplate(null,
                                                                                          this,
                                                                                          null),
                                                             new DeploymentMuleContextListenerFactory(domainDeploymentListener));
    this.domainDeployer.setDeploymentListener(domainDeploymentListener);
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public void setDefaultDomain(Domain defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  @Override
  public void start() {
    DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
    addDeploymentListener(deploymentStatusTracker.getApplicationDeploymentStatusTracker());
    addDomainDeploymentListener(deploymentStatusTracker.getDomainDeploymentStatusTracker());

    StartupSummaryDeploymentListener summaryDeploymentListener =
        new StartupSummaryDeploymentListener(deploymentStatusTracker, this);
    addStartupListener(summaryDeploymentListener);

    deployExplodedDefaultDomain();

    String[] apps = listFiles(applicationDeployer.getDeploymentDirectory(), DIRECTORY);
    if (apps.length == 0) {
      throw new IllegalStateException("No unpacked application present in apps dir.");
    }
    if (apps.length > 1) {
      throw new IllegalStateException("More than one unpacked application (" + join(", ", apps) + ") present in apps dir.");
    }
    try {
      applicationDeployer.deployExplodedArtifact(apps[0], empty());
    } catch (DeploymentException e) {
      // just rethrow, this has to make Mule exit.
      throw e;
    }

    notifyStartupListeners();
  }

  private void deployExplodedDefaultDomain() {
    try {
      domainDeployer.deployExplodedArtifact(DEFAULT_DOMAIN_NAME, empty());
    } catch (DeploymentException e) {
      logger.error("Error deploying domain '{}'", DEFAULT_DOMAIN_NAME, e);
    }
  }

  private String[] listFiles(File directory, FilenameFilter filter) {
    String[] files = directory.list(filter);
    if (files == null) {
      throw new IllegalStateException(format("We got a null while listing the contents of director '%s'. Some common " +
          "causes for this is a lack of permissions to the directory or that it's being deleted concurrently",
                                             directory.getName()));
    }
    return files;
  }

  protected void notifyStartupListeners() {
    for (StartupListener listener : startupListeners) {
      try {
        listener.onAfterStartup();
      } catch (Throwable t) {
        logger.error("Error executing startup listener {}", listener, t);
      }
    }
  }

  @Override
  public void stop() {
    if (application == null) {
      logger.warn("No application has been deployed.");
    } else {
      try {
        application.stop();
        application.dispose();
      } catch (Throwable t) {
        logger.error("Error stopping artifact {}", application.getArtifactName(), t);
      }
    }

    artifactStartExecutor.ifComputed(ExecutorService::shutdownNow);
  }

  @Override
  public Domain findDomain(String domainName) {
    // Domains are not supported in single app mode
    return null;
  }

  @Override
  public Application findApplication(String appName) {
    return application.getArtifactName().equals(appName) ? application : null;
  }

  @Override
  public Collection<Application> findDomainApplications(final String domain) {
    // Domains are not supported in single app mode
    return emptyList();
  }


  @Override
  public List<Application> getApplications() {
    return singletonList(application);
  }

  @Override
  public List<Domain> getDomains() {
    // Domains are not supported in single app mode
    return emptyList();
  }

  public void setAppFactory(ArtifactFactory<ApplicationDescriptor, Application> appFactory) {
    this.applicationDeployer.setArtifactFactory(appFactory);
  }

  @Override
  public ReentrantLock getLock() {
    return deploymentLock;
  }

  @Override
  public void undeploy(String appName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deploy(URI appArchiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deploy(URI appArchiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(String artifactName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(String artifactName, Properties appProperties) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(URI archiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(URI archiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void undeployDomain(String domainName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployDomain(URI domainArchiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeployDomain(String domainName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployDomainBundle(URI domainArchiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void addStartupListener(StartupListener listener) {
    this.startupListeners.add(listener);
  }

  @Override
  public void removeStartupListener(StartupListener listener) {
    this.startupListeners.remove(listener);
  }

  @Override
  public void addDeploymentListener(DeploymentListener listener) {
    applicationDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDeploymentListener(DeploymentListener listener) {
    applicationDeploymentListener.removeDeploymentListener(listener);
  }

  @Override
  public void addDomainDeploymentListener(DeploymentListener listener) {
    // Domains are not supported in single app mode
  }

  @Override
  public void removeDomainDeploymentListener(DeploymentListener listener) {
    // Domains are not supported in single app mode
  }

  @Override
  public void addDomainBundleDeploymentListener(DeploymentListener listener) {
    // Domains are not supported in single app mode
  }

  @Override
  public void removeDomainBundleDeploymentListener(DeploymentListener listener) {
    // Domains are not supported in single app mode
  }

  void undeploy(Application app) {
    throw new UnsupportedOperationException("");
  }

  void undeploy(Domain domain) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployDomain(URI domainArchiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeployDomain(String domainName, Properties deploymentProperties) {
    throw new UnsupportedOperationException("");
  }
}
