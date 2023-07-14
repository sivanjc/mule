/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
