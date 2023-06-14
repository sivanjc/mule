/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.api.config;

/**
 * Allows to configure the desired tracing level
 *
 * @since 4.5.0
 */
public interface TracingLevelConfiguration {

  /**
   * @return the default tracing level, MONITORING, if no other tracing level is specified from a configuration.
   */
  TracingLevel getTracingLevel();

  /**
   * If the specified location and tracing level exist, returns an override of a tracing level corresponding to a location.
   * Otherwise, returns the general tracing level if specified, or the default tracing level.
   *
   * @param location corresponds to the location of a component in a configuration.
   * @return a tracing level.
   */
  TracingLevel getTracingLevelOverride(String location);
}
