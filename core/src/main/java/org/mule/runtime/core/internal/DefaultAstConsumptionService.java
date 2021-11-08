/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.AstConsumptionService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAstConsumptionService implements AstConsumptionService {

  private static DefaultAstConsumptionService instance;
  private final Map<String, ArtifactAst> applicationNameToArtifactsAst = new ConcurrentHashMap<>();

  private DefaultAstConsumptionService() {}

  public static synchronized DefaultAstConsumptionService getInstance() {
    if (instance == null) {
      instance = new DefaultAstConsumptionService();
    }
    return instance;
  }

  @Override
  public ArtifactAst getArtifactByApplicationName(String applicationName) {
    return applicationNameToArtifactsAst.get(applicationName);
  }

  @Override
  public void registerArtifactAstByApplicationName(ArtifactAst artifactAst, String applicationName) {
    this.applicationNameToArtifactsAst.put(applicationName, artifactAst);
  }
}
