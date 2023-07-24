/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl;

import io.opentelemetry.api.trace.SpanContext;
import org.mule.runtime.tracer.exporter.impl.optel.sdk.MuleReadableSpan;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.getMutableMuleTraceStateFrom;

import java.util.Map;
import java.util.Optional;

/**
 * A context that is propagated through the trace for export.
 *
 * @since 4.5.0
 */
public class OpenTelemetryExportTraceContext {

  MuleReadableSpan parentExportableReadableSpan;

  MutableMuleTraceState traceState;
  private String rootName;
  private Map<String, String> rootAttributes = emptyMap();
  private SpanContext spanContext;

  public OpenTelemetryExportTraceContext(boolean enableMuleAncestorIdManagement) {
    this.traceState = getMutableMuleTraceStateFrom(emptyMap(), enableMuleAncestorIdManagement);
  }

  public Optional<MuleReadableSpan> getParentExportableReadableSpan() {
    return ofNullable(parentExportableReadableSpan);
  }

  public MutableMuleTraceState getTraceState() {
    return traceState;
  }

  public void setParentExportableReadableSpan(MuleReadableSpan parentExportableReadableSpan) {
    this.parentExportableReadableSpan = parentExportableReadableSpan;
  }

  public void setTraceState(MutableMuleTraceState traceState) {
    this.traceState = traceState;
  }

  public Optional<String> getRootName() {
    return ofNullable(rootName);
  }

  public Map<String, String> getRootAttributes() {
    return rootAttributes;
  }

  public void setRootName(String rootName) {
    this.rootName = rootName;
  }

  public void addRootAttribute(String key, String value) {
    this.rootAttributes.put(key, value);
  }

  public void setSpanContext(SpanContext spanContext) {
    this.spanContext = spanContext;
  }

  public SpanContext getSpanContext() {
    return spanContext;
  }
}
