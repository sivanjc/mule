/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.orchestrator.internal;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.discoverRuntimeExtensionModels;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.ast.internal.DefaultComponentParameterAst;
import org.mule.runtime.ast.internal.builder.PropertiesResolver;
import org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public class OrchestratorAstPostProcessor implements ApplicationModelAstPostProcessor {

  // We don't want to transform any component associated with the built-in extension models
  private static final Set<String> DO_NOT_REPLACE_NAMESPACES = discoverRuntimeExtensionModels().stream()
      .map(em -> em.getXmlDslModel().getNamespace())
      .collect(toSet());
  private static final String HTTP_CONNECTOR_GROUP_ID = "org.mule.connectors";
  private static final String HTTP_CONNECTOR_ARTIFACT_ID = "mule-http-connector";

  private ExtensionModel httpExtensionModel;
  private OperationModel httpRequestOperationModel;

  @Override
  public ArtifactAst postProcessAst(ArtifactAst ast, Set<ExtensionModel> extensionModels) {
    httpExtensionModel = findHttpExtensionModel(extensionModels);
    httpRequestOperationModel = findHttpRequestOperationModel(httpExtensionModel);
    return copyRecursively(ast, this::transformComponentAst);
  }

  @Override
  public Set<ComponentAst> resolveRootComponents(Collection<ComponentAst> rootComponents, Set<ExtensionModel> extensionModels) {
    // TODO: check if a non-identity implementation is actually needed
    return new HashSet<>(rootComponents);
  }

  private ExtensionModel findHttpExtensionModel(Set<ExtensionModel> extensionModels) {
    return extensionModels.stream().filter(this::isHttpExtensionModel).findFirst().orElseThrow();
  }

  private boolean isHttpExtensionModel(ExtensionModel extensionModel) {
    return extensionModel.getArtifactCoordinates()
        .map(this::isHttpExtensionModel)
        .orElse(false);
  }

  private boolean isHttpExtensionModel(ArtifactCoordinates artifactCoordinates) {
    return artifactCoordinates.getArtifactId().equals(HTTP_CONNECTOR_ARTIFACT_ID) &&
        artifactCoordinates.getGroupId().equals(HTTP_CONNECTOR_GROUP_ID);
  }

  private OperationModel findHttpRequestOperationModel(ExtensionModel extensionModel) {
    return extensionModel.getConfigurationModel("requestConfig")
        .flatMap(cm -> cm.getOperationModel("request"))
        .orElseThrow();
  }

  private ComponentAst transformComponentAst(ComponentAst componentAst) {
    ExtensionModel extensionModel = componentAst.getExtensionModel();

    if (DO_NOT_REPLACE_NAMESPACES.contains(extensionModel.getXmlDslModel().getNamespace())) {
      return componentAst;
    }

    Optional<ConfigurationModel> configurationModel = componentAst.getModel(ConfigurationModel.class);
    Optional<OperationModel> operationModel = componentAst.getModel(OperationModel.class);
    if (configurationModel.isPresent()) {
      return transformConfiguration(componentAst);
    } else if (operationModel.isPresent()) {
      return transformOperation(componentAst);
    }
    return componentAst;
  }

  private ComponentAst transformConfiguration(ComponentAst componentAst) {
    // TODO: implement
    return componentAst;
  }

  private ComponentAst transformOperation(ComponentAst componentAst) {
    return new ICaaSOperationDecorator(componentAst);
  }

  private class ICaaSOperationDecorator extends BaseComponentAstDecorator {

    private final Map<String, Map<String, ComponentParameterAst>> parameters = new LinkedHashMap<>();

    public ICaaSOperationDecorator(ComponentAst componentAst) {
      super(componentAst);
      for (ParameterGroupModel pgm : httpRequestOperationModel.getParameterGroupModels()) {
        Map<String, ComponentParameterAst> parametersFromGroup = new LinkedHashMap<>();

        for (ParameterModel pm : pgm.getParameterModels()) {
          if (pm.getName().equals("path")) {
            // TODO: actually use the correct operation
            parametersFromGroup.put(pm.getName(),
                                    createParameter("operation/Salesforce/queryAll", pm, pgm));
          } else if (pm.getName().equals("method")) {
            parametersFromGroup.put(pm.getName(), createParameter("POST", pm, pgm));
          } else if (pm.getName().equals("config-ref")) {
            parametersFromGroup.put(pm.getName(), createParameter("ICaaSConfig", pm, pgm));
          } else {
            parametersFromGroup.put(pm.getName(), createParameter(null, pm, pgm));
          }
        }

        parameters.put(pgm.getName(), parametersFromGroup);
      }
    }

    private ComponentParameterAst createParameter(String rawValue, ParameterModel parameterModel,
                                                  ParameterGroupModel parameterGroupModel) {
      return new DefaultComponentParameterAst(rawValue, parameterModel, parameterGroupModel,
                                              ComponentGenerationInformation.EMPTY_GENERATION_INFO,
                                              new PropertiesResolver());
    }

    @Override
    public List<ComponentAst> directChildren() {
      // TODO: we might need to add a body
      return emptyList();
    }

    @Override
    public Collection<ComponentParameterAst> getParameters() {
      return parameters.values().stream()
          .flatMap(params -> params.values().stream())
          .collect(toList());
    }

    @Override
    public ComponentParameterAst getParameter(String groupName, String paramName) {
      return parameters.get(groupName).get(paramName);
    }

    @Override
    public ExtensionModel getExtensionModel() {
      return httpExtensionModel;
    }

    @Override
    public <M> Optional<M> getModel(Class<M> modelClass) {
      if (modelClass.isInstance(httpRequestOperationModel)) {
        return of(modelClass.cast(httpRequestOperationModel));
      }
      return empty();
    }

    @Override
    public MetadataType getType() {
      return null;
    }

    @Override
    public ComponentIdentifier getIdentifier() {
      return buildFromStringRepresentation("http:request");
    }

    @Override
    public TypedComponentIdentifier.ComponentType getComponentType() {
      return OPERATION;
    }

    @Override
    public Optional<String> getComponentId() {
      return empty();
    }
  }
}
