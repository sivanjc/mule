/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.tuning;

import org.mule.runtime.tracer.api.tuning.TracingLevel;
import org.mule.runtime.tracer.api.tuning.TracingTuningConfiguration;

public class DefaultTracingTuningConfiguration implements TracingTuningConfiguration {

  @Override
  public TracingLevel getLevel() {
    return TracingLevel.LEVEL_2;
  }
}
