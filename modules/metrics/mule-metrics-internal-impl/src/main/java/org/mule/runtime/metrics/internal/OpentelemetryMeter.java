/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.internal;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.mule.runtime.metrics.api.LongCounter;
import org.mule.runtime.metrics.api.Meter;

public class OpentelemetryMeter implements Meter {

  private final String name;

  private static MeterProvider meterProvider;

  static {
    OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().buildAndRegisterGlobal();
    meterProvider = openTelemetrySdk.getMeterProvider();
  }

  private final io.opentelemetry.api.metrics.Meter meter;

  public OpentelemetryMeter(String name) {
    this.name = name;
    this.meter = meterProvider.get("mule.runtime");
  }

  @Override
  public LongCounter getLongCounter(String name) {
    return new OpentelemetryLogCounter(name, this);
  }

  io.opentelemetry.api.metrics.Meter getOpentelemetryMeter() {
    return meter;
  }
}
