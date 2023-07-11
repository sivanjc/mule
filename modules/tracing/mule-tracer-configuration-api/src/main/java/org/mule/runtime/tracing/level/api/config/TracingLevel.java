/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracing.level.api.config;

public class TracingLevel {

  private final boolean isOverride;
  private final TracingLevelId tracingLevelId;

  public TracingLevel(boolean isOverride, TracingLevelId tracingLevelId) {
    this.isOverride = isOverride;
    this.tracingLevelId = tracingLevelId;
  }

  public boolean isOverride() {
    return isOverride;
  }

  public TracingLevelId getTracingLevelId() {
    return tracingLevelId;
  }
}
