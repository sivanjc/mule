/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.core.internal.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.java.CraftedExtensionModelLoader.CRAFTED_LOADER_ID;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Splitter;

/**
 * Creates {@link ArtifactPluginDescriptor} instances
 */
public class ArtifactPluginDescriptorFactory
    extends AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> {

  /**
   * Allows to set an extension model loader for a mule-plugin when one wasn't defined in the plugin mule-artifact.json
   * descriptor.
   * 
   * @since 4.4
   */
  private static final String CRAFTED_FALLBACK_KEY =
      SYSTEM_PROPERTY_PREFIX + "extensionModelLoaderDescriptor." + CRAFTED_LOADER_ID + "Fallback";

  /**
   * Creates a default factory
   */
  public ArtifactPluginDescriptorFactory() {
    this(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
         ArtifactDescriptorValidatorBuilder.builder());
  }

  /**
   * Creates a custom factory
   * 
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderModelLoader} registered on the container. Non
   *                                           null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} builder to define the validator to be
   *                                           used. Non null.
   */
  public ArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                         ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
  }

  @Override
  public ArtifactPluginDescriptor create(File pluginJarFile, Optional<Properties> deploymentProperties)
      throws ArtifactDescriptorCreateException {
    try {
      checkArgument(pluginJarFile.isDirectory() || pluginJarFile.getName().endsWith(".jar"),
                    "provided file is not a plugin: " + pluginJarFile.getAbsolutePath());
      // Use / instead of File.separator as the file is going to be accessed inside the jar as a URL
      String mulePluginJsonPathInsideJarFile = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR;
      Optional<byte[]> jsonDescriptorContentOptional = loadFileContentFrom(pluginJarFile, mulePluginJsonPathInsideJarFile);
      return jsonDescriptorContentOptional
          .map(jsonDescriptorContent -> loadFromJsonDescriptor(pluginJarFile,
                                                               loadModelFromJson(new String(jsonDescriptorContent)),
                                                               deploymentProperties))
          .orElseThrow(() -> new ArtifactDescriptorCreateException(pluginDescriptorNotFound(pluginJarFile,
                                                                                            mulePluginJsonPathInsideJarFile)));
    } catch (ArtifactDescriptorCreateException e) {
      throw e;
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  @Override
  protected Map<String, Object> getClassLoaderModelAttributes(Optional<Properties> deploymentPropertiesOptional,
                                                              MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                                              BundleDescriptor bundleDescriptor) {
    Map<String, Object> attributes =
        super.getClassLoaderModelAttributes(deploymentPropertiesOptional, classLoaderModelLoaderDescriptor, bundleDescriptor);

    if (deploymentPropertiesOptional.isPresent()) {
      Properties deploymentProperties = deploymentPropertiesOptional.get();
      if (deploymentProperties instanceof PluginExtendedDeploymentProperties) {
        PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
            (PluginExtendedDeploymentProperties) deploymentProperties;
        return new PluginExtendedClassLoaderModelAttributes(attributes,
                                                            pluginExtendedDeploymentProperties.getDeployableArtifactDescriptor());
      }
    }
    return attributes;
  }

  @Override
  protected Map<String, Object> getBundleDescriptorAttributes(MuleArtifactLoaderDescriptor bundleDescriptorLoader,
                                                              Optional<Properties> deploymentPropertiesOptional) {
    Map<String, Object> attributes =
        super.getBundleDescriptorAttributes(bundleDescriptorLoader, deploymentPropertiesOptional);

    if (deploymentPropertiesOptional.isPresent()) {
      Properties deploymentProperties = deploymentPropertiesOptional.get();
      if (deploymentProperties instanceof PluginExtendedDeploymentProperties) {
        PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
            (PluginExtendedDeploymentProperties) deploymentProperties;
        return new PluginExtendedBundleDescriptorAttributes(attributes,
                                                            pluginExtendedDeploymentProperties.getPluginBundleDescriptor());
      }
    }
    return attributes;
  }

  @Override
  protected ArtifactType getArtifactType() {
    return PLUGIN;
  }

  @Override
  protected void doDescriptorConfig(MulePluginModel artifactModel, ArtifactPluginDescriptor descriptor, File artifactLocation) {
    artifactModel.getExtensionModelLoaderDescriptor().ifPresent(extensionModelDescriptor -> {
      final LoaderDescriber loaderDescriber = new LoaderDescriber(extensionModelDescriptor.getId());
      loaderDescriber.addAttributes(extensionModelDescriptor.getAttributes());
      descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    });

    if (!artifactModel.getExtensionModelLoaderDescriptor().isPresent()) {
      String craftedFallbacks = getProperty(CRAFTED_FALLBACK_KEY);
      if (craftedFallbacks != null) {
        Map<String, String> craftedFallback = Splitter.on(";").withKeyValueSeparator('=').split(craftedFallbacks);
        String pluginFallback = craftedFallback.get(descriptor.getBundleDescriptor().getGroupId() + ":"
            + descriptor.getBundleDescriptor().getArtifactId() + ":" + descriptor.getBundleDescriptor().getVersion());

        if (pluginFallback != null) {
          LoaderDescriber loaderDescriber = new LoaderDescriber(CRAFTED_LOADER_ID);
          loaderDescriber.addAttributes(singletonMap(TYPE_PROPERTY_NAME, pluginFallback));

          descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
        }
      }
    }

    artifactModel.getLicense().ifPresent(descriptor::setLicenseModel);
  }

  @Override
  protected ArtifactPluginDescriptor createArtifactDescriptor(File artifactLocation, String name,
                                                              Optional<Properties> deploymentProperties) {
    return new ArtifactPluginDescriptor(name, deploymentProperties);
  }

  private static String pluginDescriptorNotFound(File pluginFile, String mulePluginJsonPathInsideJarFile) {
    return format("The plugin descriptor '%s' on plugin file '%s' is not present", mulePluginJsonPathInsideJarFile, pluginFile);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MulePluginModel> getMuleArtifactModelJsonSerializer() {
    return new MulePluginModelJsonSerializer();
  }

}
