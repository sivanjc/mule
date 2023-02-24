/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.tuning;


import org.mule.runtime.tracer.api.tuning.TracingLevel;
import org.mule.runtime.tracer.api.tuning.TracingTunable;
import org.mule.runtime.tracer.api.tuning.TracingTuningConfiguration;
import org.mule.runtime.tracer.api.tuning.TracingTuningStrategy;

import javax.inject.Inject;

public class DefaultTracingTuningStrategy implements TracingTuningStrategy {

  @Inject
  private TracingTuningConfiguration tracingTuningConfiguration;

  @Override
  public void tune(TracingTunable tracingTunable, int level) {
    if (tracingTuningConfiguration.getLevel().equals(TracingLevel.LEVEL_2)) {
      if (level > 1) {
        tracingTunable.tune(true);
      }
    } else if (tracingTuningConfiguration.getLevel().equals(TracingLevel.LEVEL_1)) {
      tracingTunable.tune(true);
    }
  }
}
