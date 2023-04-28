package org.mule.runtime.module.orchestrator.internal;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;

import java.util.Collection;
import java.util.Set;

public class OrchestratorAstPostProcessor implements ApplicationModelAstPostProcessor {

  @Override
  public ArtifactAst postProcessAst(ArtifactAst ast, Set<ExtensionModel> extensionModels) {
    // TODO: implement
    return null;
  }

  @Override
  public Set<ComponentAst> resolveRootComponents(Collection<ComponentAst> rootComponents, Set<ExtensionModel> extensionModels) {
    // TODO: implement
    return null;
  }
}