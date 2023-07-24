/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.customization.impl.export.TracingLevelExportInfo;
import org.mule.runtime.tracer.customization.impl.info.ExecutionInitialSpanInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.5.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider {

  private final Map<InitialSpanInfoIdentifier, ExecutionInitialSpanInfo> initialSpanInfos = new ConcurrentHashMap<>();
  public static final String API_ID_CONFIGURATION_PROPERTIES_KEY = "apiId";

  @Inject
  MuleContext muleContext;

  @Inject
  ConfigurationProperties configurationProperties;

  @Inject
  TracingLevelConfiguration tracingLevelConfiguration;

  private String apiId;
  private boolean initialisedAttributes;
  private String overriddenName;
  private String suffix;

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component) {
    // TODO: Verify initialisation order in mule context (W-12761329)
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    // Mover esta logica a TracingLevelExportInfo.createTracingLevelExportInfo
    return initialSpanInfos.computeIfAbsent(getInitialSpanInfoIdentifier(component, "", ""),
                                            identifier -> createExecutionInitialSpanInfo(component, "", ""));
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    // Mover esta logica a TracingLevelExportInfo.createTracingLevelExportInfo
    return initialSpanInfos.computeIfAbsent(getInitialSpanInfoIdentifier(component, suffix, ""),
                                            identifier -> createExecutionInitialSpanInfo(component, "", suffix));
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    // Mover esta logica a TracingLevelExportInfo.createTracingLevelExportInfo
    return initialSpanInfos.computeIfAbsent(getInitialSpanInfoIdentifier(component, suffix, overriddenName),
                                            identifier -> createExecutionInitialSpanInfo(component, overriddenName, suffix));
  }

  private ExecutionInitialSpanInfo createExecutionInitialSpanInfo(Component component, String overriddenName, String suffix) {
    this.overriddenName = overriddenName;
    this.suffix = suffix;
    ExecutionInitialSpanInfo executionInitialSpanInfo =
        new ExecutionInitialSpanInfo(component, apiId, overriddenName, suffix, tracingLevelConfiguration);
    return executionInitialSpanInfo;
  }

  public void initialiseAttributes() {
    if (muleContext.getArtifactType().equals(POLICY)) {
      apiId = configurationProperties.resolveStringProperty(API_ID_CONFIGURATION_PROPERTIES_KEY).orElse(null);
    }
  }

  private InitialSpanInfoIdentifier getInitialSpanInfoIdentifier(Component location, String suffix, String overriddenName) {
    return new InitialSpanInfoIdentifier(location, suffix, overriddenName);
  }
}
