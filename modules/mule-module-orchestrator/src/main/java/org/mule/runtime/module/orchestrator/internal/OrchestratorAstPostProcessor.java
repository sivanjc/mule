/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.orchestrator.internal;

import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.discoverRuntimeExtensionModels;

import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class OrchestratorAstPostProcessor implements ApplicationModelAstPostProcessor {

  // We don't want to transform any component associated with the built-in extension models
  private static final Set<String> DO_NOT_REPLACE_NAMESPACES = discoverRuntimeExtensionModels().stream()
      .map(em -> em.getXmlDslModel().getNamespace())
      .collect(toSet());

  @Override
  public ArtifactAst postProcessAst(ArtifactAst ast, Set<ExtensionModel> extensionModels) {
    return copyRecursively(ast, this::transformComponentAst);
  }

  @Override
  public Set<ComponentAst> resolveRootComponents(Collection<ComponentAst> rootComponents, Set<ExtensionModel> extensionModels) {
    // TODO: check if a non-identity implementation is actually needed
    return new HashSet<>(rootComponents);
  }

  private ComponentAst transformComponentAst(ComponentAst componentAst) {
    ExtensionModel extensionModel = componentAst.getExtensionModel();

    if (DO_NOT_REPLACE_NAMESPACES.contains(extensionModel.getXmlDslModel().getNamespace())) {
      return componentAst;
    }

    Optional<ConfigurationModel> configurationModel = componentAst.getModel(ConfigurationModel.class);
    Optional<OperationModel> operationModel = componentAst.getModel(OperationModel.class);
    if (configurationModel.isPresent()) {
      transformConfiguration(componentAst);
    } else if (operationModel.isPresent()) {
      transformOperation(componentAst);
    }
    return componentAst;
  }

  private ComponentAst transformConfiguration(ComponentAst componentAst) {
    // TODO: implement
    return componentAst;
  }

  private ComponentAst transformOperation(ComponentAst componentAst) {
    // TODO: implement
    return componentAst;
  }
}
