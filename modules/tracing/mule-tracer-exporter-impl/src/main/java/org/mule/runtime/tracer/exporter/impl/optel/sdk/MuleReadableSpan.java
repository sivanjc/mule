/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.getMutableMuleTraceStateFrom;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_LIBRARY_INFO;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_SCOPE_INFO;

import static java.util.Collections.emptyMap;

import static io.opentelemetry.api.trace.SpanContext.getInvalid;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporter;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.api.trace.TraceFlags;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils;

/**
 * An implementation of OTEL sdk {@link ReadableSpan}.
 *
 * @since 4.5.0
 */
public class MuleReadableSpan implements ReadableSpan {

  public static MuleReadableSpanBuilder muleReadableSpanBuilderFor(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
    return new MuleReadableSpanBuilder(openTelemetrySpanExporter);
  }

  private final boolean exportable;
  private SpanContext parentSpanContext = getInvalid();

  private MutableMuleTraceState muleTraceState;

  private LazyValue<SpanContext> spanContext = new LazyValue<>(this::createSpanContext);

  private final OpenTelemetrySpanExporter openTelemetrySpanExporter;
  private final MuleSpanData spanData;


  public MuleReadableSpan(OpenTelemetrySpanExporter openTelemetrySpanExporter, Resource resource, String artifactId,
                          String artifactType, InitialExportInfo initialExportInfo,
                          boolean enableMuleAncestorIdManagement) {
    this.openTelemetrySpanExporter = openTelemetrySpanExporter;
    this.spanData = new MuleSpanData(this, resource, artifactId, artifactType);
    this.exportable = initialExportInfo.isExportable();
    this.muleTraceState = getMutableMuleTraceStateFrom(emptyMap(), enableMuleAncestorIdManagement);
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext.get();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return parentSpanContext;
  }

  @Override
  public String getName() {
    return openTelemetrySpanExporter.getName();
  }

  @Override
  public SpanData toSpanData() {
    return spanData;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return INSTRUMENTATION_LIBRARY_INFO;
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return INSTRUMENTATION_SCOPE_INFO;
  }

  @Override
  public boolean hasEnded() {
    return true;
  }

  @Override
  public long getLatencyNanos() {
    return 0;
  }

  @Override
  public SpanKind getKind() {
    return openTelemetrySpanExporter.getKind();
  }

  @Override
  public <T> T getAttribute(AttributeKey<T> key) {
    throw new UnsupportedOperationException();
  }

  public OpenTelemetrySpanExporter getOpenTelemetrySpanExporter() {
    return openTelemetrySpanExporter;
  }

  private SpanContext createSpanContext() {
    // Generates the span id so that the OpenTelemetry spans can be lazily initialised if it is exportable
    if (exportable) {
      String spanId = OpenTelemetryTraceIdUtils.generateSpanId();
      String traceId = OpenTelemetryTraceIdUtils.generateTraceId(parentSpanContext);
      return SpanContext.create(traceId, spanId, TraceFlags.getSampled(), muleTraceState);
    } else {
      return SpanContext.getInvalid();
    }
  }

  public void updateName(String name) {
    spanData.updateName(name);
  }

  public void addAttribute(String entry, String key) {
    spanData.addAttribute(entry, key);
  }

  public void setParentSpanContext(SpanContext parentSpanContext) {
    this.parentSpanContext = parentSpanContext;
  }

  public static class MuleReadableSpanBuilder {

    private final OpenTelemetrySpanExporter openTelemetrySpanExporter;
    private String artifactId;
    private String artifactType;
    private Resource resource;
    private boolean export;
    private boolean enableMuleAncestorIdManagement;
    private InitialExportInfo initialExportInfo;

    public MuleReadableSpanBuilder(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
      this.openTelemetrySpanExporter = openTelemetrySpanExporter;
    }

    public MuleReadableSpanBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public MuleReadableSpanBuilder withArtifactType(String artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public MuleReadableSpanBuilder withResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public MuleReadableSpanBuilder export(boolean export) {
      this.export = export;
      return this;
    }

    public MuleReadableSpanBuilder enableMuleAncestorIdManagement(boolean enableMuleAncestorIdManagement) {
      this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
      return this;
    }

    public MuleReadableSpanBuilder withInitialExportInfo(InitialExportInfo initialExportInfo) {
      this.initialExportInfo = initialExportInfo;
      return this;
    }

    public MuleReadableSpan build() {
      return new MuleReadableSpan(openTelemetrySpanExporter, resource, artifactId, artifactType, initialExportInfo,
                                  enableMuleAncestorIdManagement);
    }

  }

  public boolean isExportable() {
    return exportable;
  }
}
