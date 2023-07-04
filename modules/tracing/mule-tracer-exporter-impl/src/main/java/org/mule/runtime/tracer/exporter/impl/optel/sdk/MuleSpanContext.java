/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

public class MuleSpanContext implements SpanContext {

  private final SpanContext delegate;
  private String traceId;

  public MuleSpanContext(SpanContext delegate) {
    this.delegate = delegate;
    this.traceId = delegate.getTraceId();
  }

  @Override
  public String getTraceId() {
    return traceId;
  }

  @Override
  public String getSpanId() {
    return delegate.getSpanId();
  }

  @Override
  public TraceFlags getTraceFlags() {
    return delegate.getTraceFlags();
  }

  @Override
  public TraceState getTraceState() {
    return delegate.getTraceState();
  }

  @Override
  public boolean isRemote() {
    return delegate.isRemote();
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }
}
