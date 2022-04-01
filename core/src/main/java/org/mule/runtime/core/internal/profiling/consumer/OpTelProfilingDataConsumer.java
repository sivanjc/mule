/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.END_SPAN;

import static java.time.Instant.ofEpochMilli;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

@RuntimeInternalProfilingDataConsumer
public class OpTelProfilingDataConsumer implements ProfilingDataConsumer<SpanProfilingEventContext> {

  public static final String SERVICE_NAME = "mule-tracing";

  static {
    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().setEndpoint("http://0.0.0.0:4317").build())
            .build())
        .build();

    OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();
  }

  private final Tracer TRACER = GlobalOpenTelemetry.getTracer(SERVICE_NAME, "0.0.1");

  @Override
  public void onProfilingEvent(ProfilingEventType<SpanProfilingEventContext> profilingEventType,
                               SpanProfilingEventContext profilingEventContext) {

    TRACER
        .spanBuilder(profilingEventContext.getSpan().getName())
        .setStartTimestamp(ofEpochMilli(profilingEventContext.getSpan().getDuration().getStart()))
        .startSpan()
        .end(ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
  }

  @Override
  public Set<ProfilingEventType<SpanProfilingEventContext>> getProfilingEventTypes() {
    return ImmutableSet.of(END_SPAN);
  }

  @Override
  public Predicate<SpanProfilingEventContext> getEventContextFilter() {
    return evContext -> true;
  }
}
