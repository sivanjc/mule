/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.internal;

import org.mule.runtime.metrics.api.Meter;
import org.mule.runtime.metrics.api.MeterProvider;

public class OpentelemetryMeterProvider implements MeterProvider {

  @Override
  public Meter getMeter(String name) {
    return new OpentelemetryMeter(name);
  }
}
