/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.ofNullable;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.api.model.RemoteRepository.newRemoteRepositoryBuilder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.setMavenConfig;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DEPLOYMENT_TYPE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.filefilter.HiddenFileFilter.VISIBLE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import io.qameta.allure.Description;
import org.mule.maven.client.api.model.MavenConfiguration.MavenConfigurationBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Feature;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(DEPLOYMENT_TYPE)
public class DeploymentTypeAppControlTestCase extends AbstractApplicationDeploymentTestCase {

  private static final int ERROR_LEVEL = 1;
  private static final String EXCEPTION_ERROR_MESSAGE =
      "org.eclipse.aether.transfer.ArtifactNotFoundException: Could not find artifact org.mule.connectors:mule-sockets-connector";
  private static final String APP_XML_FILE = "simple.xml";
  private static final String HEAVYWEIGHT_APP = "heavyweight";
  private static final String LIGHTWEIGHT_APP = "lightweight";
  private static final String MULE_CONNECTORS_GROUP_ID = "org.mule.connectors";
  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_PLUGIN_EXTENSION_NAME = "-mule-plugin.jar";
  private static final String MULE_PLUGIN_NAME = "mule-sockets-connector";
  private static final String MULESOFT_PUBLIC_REPOSITORY = "https://repository.mulesoft.org/nexus/content/repositories/public/";
  private static final TestLogger logger = getTestLogger(DefaultArchiveDeployer.class);
  public static final String MULE_PLUGIN_VERSION = "1.2.0";
  public static final String MULE_CORE_DEPENDENCY = "mule-core";
  public static final String JAR = ".jar";
  public static final String MAVEN_REPO_ID = "mulesoft-public";
  public static final String REPOSITORY_PATH = "repository";
  private final String appWeight;
  private final boolean applicationRepositoryMustExist;
  private final boolean mulePluginMustBeResolved;

  @Parameterized.Parameters(
      name = "Parallel: {0}, AppWeight: {1}, Application Repository Must Exist: {2}, Mule Plugin Must be Resolved: {3}")
  public static List<Object[]> parameters() {
    return Arrays
        .asList(new Object[][] {
            {FALSE, HEAVYWEIGHT_APP, TRUE, TRUE},
            {FALSE, LIGHTWEIGHT_APP, FALSE, FALSE},
            {FALSE, LIGHTWEIGHT_APP, FALSE, TRUE},
        });
  }

  public DeploymentTypeAppControlTestCase(boolean parallelDeployment, String appWeight,
                                          boolean applicationRepositoryMustExist, boolean mulePluginMustBeResolved) {
    super(parallelDeployment);
    this.appWeight = appWeight;
    this.applicationRepositoryMustExist = applicationRepositoryMustExist;
    this.mulePluginMustBeResolved = mulePluginMustBeResolved;
  }

  @Test
  @Description("Verifies that the parameterized app has the correct deployment result according to the configuration and its deployment type.")
  public void appDeploymentTypeControlTest() throws Exception {
    // Creates app for testing
    final ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder(appWeight);
    addPackedAppFromBuilder(applicationFileBuilder);

    // Configures maven repository
    File mavenMuleRepository = getMavenMuleRepository(appWeight);

    // Deploys the app
    startDeployment();

    // Asserts the expected deployment result
    assertDeploymentTypeCorrectlyManaged(mavenMuleRepository, applicationFileBuilder.getDeployedPath(),
                                         applicationFileBuilder.getId());
  }

  private void assertDeploymentTypeCorrectlyManaged(File muleRepository, String applicationName, String applicationId) {
    final File applicationRepository = Paths.get(getAppFolder(applicationName).toString(), "repository").toFile();

    assertExpectedDeploymentResult(applicationName, applicationId, applicationRepository);

    assertMuleRepositoryExpectedContent(muleRepository);
  }

  private void assertMuleRepositoryExpectedContent(File muleRepository) {
    if (appWeight.equals(LIGHTWEIGHT_APP) && mulePluginMustBeResolved) {
      final Collection<File> muleRepositoryContents = getRepositoryContents(muleRepository);
      final List<String> muleRepositoryContentNames = muleRepositoryContents.stream().map(File::getName).collect(toList());
      assertThat(muleRepositoryContentNames, hasItem(allOf(startsWith(MULE_PLUGIN_NAME), endsWith(MULE_PLUGIN_EXTENSION_NAME))));
      assertThat(muleRepositoryContentNames, not(hasItem(allOf(startsWith(MULE_CORE_DEPENDENCY), endsWith(JAR)))));
    }
  }

  private File getMavenMuleRepository(String appWeight) throws MalformedURLException {
    final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), REPOSITORY_PATH).toFile();

    if (appWeight.equals(LIGHTWEIGHT_APP) && mulePluginMustBeResolved) {
      setMavenConfiguration(muleRepository);
    }

    return muleRepository;
  }

  private void setMavenConfiguration(File repository) throws MalformedURLException {
    MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder();
    mavenConfigurationBuilder.remoteRepository(newRemoteRepositoryBuilder()
        .id(MAVEN_REPO_ID)
        .url(new URL(MULESOFT_PUBLIC_REPOSITORY))
        .build());
    mavenConfigurationBuilder.localMavenRepositoryLocation(repository);
    setMavenConfig(mavenConfigurationBuilder.build());
  }

  private void assertExpectedDeploymentResult(String applicationName, String applicationId, File applicationRepository) {
    if (!mulePluginMustBeResolved) {
      assertThat(getLogCauseMessages(logger.getAllLoggingEvents()), hasItem(startsWith(EXCEPTION_ERROR_MESSAGE)));
    } else {
      assertDeploymentSuccess(applicationDeploymentListener, applicationId);
      assertApplicationAnchorFileExists(applicationName);
      assertThat(applicationRepository.exists(), is(applicationRepositoryMustExist));
    }
  }

  private ApplicationFileBuilder getApplicationFileBuilder(String applicationWeight) {
    ArtifactPluginFileBuilder pluginBuilder = new ArtifactPluginFileBuilder(MULE_PLUGIN_NAME)
        .withGroupId(MULE_CONNECTORS_GROUP_ID)
        .withVersion(MULE_PLUGIN_VERSION)
        .withClassifier(MULE_PLUGIN_CLASSIFIER)
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo");

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder(applicationWeight)
        .definedBy(APP_XML_FILE)
        .dependingOn(pluginBuilder);

    if (applicationWeight.equals(LIGHTWEIGHT_APP)) {
      applicationFileBuilder.usingLightWeightPackage();
    }

    return applicationFileBuilder;
  }

  private Collection<File> getRepositoryContents(File repository) {
    if (repository.exists() && repository.isDirectory()) {
      List<File> repositoryFiles = new LinkedList<>();

      final Iterator<File> iterateFiles = iterateFiles(repository, VISIBLE, VISIBLE);
      while (iterateFiles.hasNext()) {
        File currentFile = iterateFiles.next();
        if (currentFile.isFile()) {
          repositoryFiles.add(currentFile);
        }
      }

      return repositoryFiles;
    } else {
      throw new NoSuchElementException("No internal repository found");
    }
  }

  private List<String> getLogCauseMessages(List<LoggingEvent> loggingEvents) {
    List<String> logCauseMessages = new LinkedList<>();
    Optional<Throwable> logCause = loggingEvents.get(ERROR_LEVEL).getThrowable();

    while (logCause.isPresent()) {
      logCauseMessages.add(logCause.get().toString());
      logCause = ofNullable(logCause.get().getCause());
    }

    return logCauseMessages;
  }
}
