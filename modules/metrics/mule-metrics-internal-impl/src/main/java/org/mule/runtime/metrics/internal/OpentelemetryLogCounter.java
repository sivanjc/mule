/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.internal;

import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import org.mule.runtime.metrics.api.LongCounter;

public class OpentelemetryLogCounter implements LongCounter {

  private final String name;
  private final OpentelemetryMeter openTelemetryMeter;
  private final io.opentelemetry.api.metrics.LongCounter openTelemetryLogCounter;

  private static final SdkMeterProvider meterProvider;

  static {
    meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
        .build();
  }

  public OpentelemetryLogCounter(String name, OpentelemetryMeter opentelemetryMeter) {
    this.name = name;
    this.openTelemetryMeter = opentelemetryMeter;
    // this.openTelemetryLogCounter = opentelemetryMeter.getOpentelemetryMeter().counterBuilder(name).build();
    openTelemetryLogCounter = meterProvider.get("instrumentation").counterBuilder(name).build();
  }

  @Override
  public void add(long value) {
    this.openTelemetryLogCounter.add(value);
  }
}
