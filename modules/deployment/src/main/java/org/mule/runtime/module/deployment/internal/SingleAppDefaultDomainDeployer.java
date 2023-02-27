/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

public class SingleAppDefaultDomainDeployer extends DefaultArchiveDeployer<DomainDescriptor, Domain>
    implements ArchiveDeployer<DomainDescriptor, Domain> {

  public SingleAppDefaultDomainDeployer(ArtifactDeployer<Domain> domainMuleDeployer, DefaultDomainFactory domainFactory,
                                        Consumer<Domain> domains, DomainDeploymentTemplate domainDeploymentTemplate,
                                        DeploymentMuleContextListenerFactory deploymentMuleContextListenerFactory) {
    super(domainMuleDeployer, domainFactory, new ObservableList<>(), domainDeploymentTemplate,
          deploymentMuleContextListenerFactory);
  }

  @Override
  public Domain deployExplodedArtifact(String artifactDir, Optional<Properties> deploymentProperties) {
    return super.deployExplodedArtifact(artifactDir, deploymentProperties);
  }
}
