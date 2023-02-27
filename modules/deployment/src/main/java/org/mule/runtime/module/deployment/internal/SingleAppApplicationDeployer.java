/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.containsType;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deployer of an artifact within Mule container. - Just deploys a single exploded app
 */
public class SingleAppApplicationDeployer
    implements ArchiveDeployer<ApplicationDescriptor, Application> {

  public static final String ARTIFACT_NAME_PROPERTY = "artifactName";
  public static final String JAR_FILE_SUFFIX = ".jar";
  public static final String ZIP_FILE_SUFFIX = ".zip";
  private static final Logger logger = LoggerFactory.getLogger(SingleAppApplicationDeployer.class);
  static final String START_ARTIFACT_ON_DEPLOYMENT_PROPERTY = "startArtifactOnDeployment";

  private final ArtifactDeployer<Application> deployer;
  private final File artifactDir;
  private final Consumer<Application> artifactCallback;
  private AbstractDeployableArtifactFactory<ApplicationDescriptor, Application> artifactFactory;
  private DeploymentListener deploymentListener = new NullDeploymentListener();
  private final MuleContextListenerFactory muleContextListenerFactory;


  public SingleAppApplicationDeployer(final ArtifactDeployer<Application> deployer,
                                      final AbstractDeployableArtifactFactory<ApplicationDescriptor, Application> artifactFactory,
                                      final Consumer<Application> artifactCallback,
                                      MuleContextListenerFactory muleContextListenerFactory) {
    this.deployer = deployer;
    this.artifactFactory = artifactFactory;
    this.artifactCallback = artifactCallback;
    this.artifactDir = artifactFactory.getArtifactDir();
    this.muleContextListenerFactory = muleContextListenerFactory;
  }

  @Override
  public boolean isUpdatedZombieArtifact(String artifactName) {
    // TODO
    return false;
  }

  @Override
  public void undeployArtifact(String artifactId) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public File getDeploymentDirectory() {
    return artifactFactory.getArtifactDir();
  }

  @Override
  public Map<String, Map<URI, Long>> getArtifactsZombieMap() {
    return emptyMap();
  }

  @Override
  public void setArtifactFactory(final ArtifactFactory<ApplicationDescriptor, Application> artifactFactory) {
    if (!(artifactFactory instanceof AbstractDeployableArtifactFactory)) {
      throw new IllegalArgumentException("artifactFactory is expected to be of type "
          + AbstractDeployableArtifactFactory.class.getName());
    }
    this.artifactFactory = (AbstractDeployableArtifactFactory<ApplicationDescriptor, Application>) artifactFactory;
  }

  @Override
  public void undeployArtifactWithoutUninstall(Application artifact) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void setDeploymentListener(DeploymentListener deploymentListener) {
    this.deploymentListener = deploymentListener;
  }

  private Application createArtifact(File artifactLocation, Optional<Properties> appProperties) throws IOException {
    Application artifact = artifactFactory.createArtifact(artifactLocation, appProperties);
    artifact.setMuleContextListener(muleContextListenerFactory.create(artifact.getArtifactName()));
    return artifact;
  }

  @Override
  public Application deployPackagedArtifact(String zip, Optional<Properties> deploymentProperties) throws DeploymentException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public Application deployPackagedArtifact(URI artifactAchivedUri, Optional<Properties> appProperties)
      throws DeploymentException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(String artifactName, Optional<Properties> deploymentProperties) throws DeploymentException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployArtifact(Application artifact, Optional<Properties> deploymentProperties) throws DeploymentException {
    throw new UnsupportedOperationException("");
  }

  public void deployArtifact(Application artifact, Optional<Properties> deploymentProperties,
                             // TODO what about these artifactStatusProperties?
                             Optional<Properties> artifactStatusProperties)
      throws DeploymentException {
    try {
      // add to the list of known artifacts first to avoid deployment loop on failure
      trackArtifact(artifact);

      deploymentListener.onDeploymentStart(artifact.getArtifactName());
      deployer
          .deploy(artifact,
                  shouldStartArtifactAccordingToStatusBeforeDomainRedeployment(artifact, artifactStatusProperties.orElse(null)));

      // artifactArchiveInstaller.createAnchorFile(artifact.getArtifactName());
      deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
      // artifactZombieMap.remove(artifact.getArtifactName());
    } catch (Throwable t) {
      // error text has been created by the deployer already
      if (containsType(t, DeploymentStartException.class)) {
        log(miniSplash(format("Failed to deploy artifact '%s', see artifact's log for details",
                              artifact.getArtifactName())));
        logger.error(t.getMessage(), t);
      } else {
        log(miniSplash(format("Failed to deploy artifact '%s', %s", artifact.getArtifactName(), t.getCause().getMessage())));
        logger.error(t.getMessage(), t);
      }

      // addZombieApp(artifact);

      deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
      if (t instanceof DeploymentException) {
        throw (DeploymentException) t;
      } else {
        throw new DeploymentException(createStaticMessage("Failed to deploy artifact: " + artifact.getArtifactName()), t);
      }
    }
  }

  /**
   * Checks the stored but not persisted property START_ARTIFACT_ON_DEPLOYMENT_PROPERTY to know if the artifact should be started
   * or not. If the artifact was purposely stopped and then its domain was redeployed, the artifact should maintain its status and
   * not start on deployment.
   */
  private boolean shouldStartArtifactAccordingToStatusBeforeDomainRedeployment(Application artifact,
                                                                               Properties artifactStatusProperties) {
    if (!(artifact instanceof Application) || artifactStatusProperties == null) {
      return true;
    }

    return valueOf(artifactStatusProperties.getProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, "true"));
  }

  @Override
  public Application deployExplodedArtifact(String artifactDir, Optional<Properties> deploymentProperties) {
    return deployExplodedApp(artifactDir, deploymentProperties, empty());
  }

  private Application deployExplodedApp(String addedApp, Optional<Properties> deploymentProperties,
                                        Optional<Properties> artifactStatusProperties)
      throws DeploymentException {
    if (logger.isDebugEnabled()) {
      logger.debug("================== New Exploded Artifact: " + addedApp);
    }

    Application artifact;
    try {
      File artifactLocation = new File(artifactDir, addedApp);
      artifact = createArtifact(artifactLocation, deploymentProperties);

      // add to the list of known artifacts first to avoid deployment loop on failure
      trackArtifact(artifact);
    } catch (Throwable t) {
      if (containsType(t, DeploymentStartException.class)) {
        log(miniSplash(format("Failed to deploy artifact '%s', see artifact's log for details", addedApp)));
        logger.error(t.getMessage());
      } else {
        log(miniSplash(format("Failed to deploy artifact '%s', see below", addedApp)));
        logger.error(t.getMessage(), t);
      }

      deploymentListener.onDeploymentFailure(addedApp, t);

      if (t instanceof DeploymentException) {
        throw (DeploymentException) t;
      } else {
        throw new DeploymentException(createStaticMessage("Failed to deploy artifact: " + addedApp), t);
      }
    }

    deployArtifact(artifact, deploymentProperties, artifactStatusProperties);
    return artifact;
  }

  private void trackArtifact(Application artifact) {
    artifactCallback.accept(artifact);
  }

  @Override
  public void doNotPersistArtifactStop(Application artifact) {
    deployer.doNotPersistArtifactStop(artifact);
  }

}
