/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.TraceContext;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class PropagatedTraceContext implements TraceContext {

  private final String nameSetBySource;
  private final Map<String, String> attributesSetBySource;
  private InternalSpan lastExportableSpan;

  private InitialExportInfo initialExportInfo;
  private Map<String, String> traceState;

  public static TraceContext from(TraceContext traceContext) {
    return new PropagatedTraceContext(traceContext.getTraceId(), traceContext.getSpanId(),
                                      traceContext.getRemoteTraceSerialization(),
                                      traceContext.getNameSetBySource().orElse(null),
                                      traceContext.getAttributesSetBySource(),
                                      traceContext.getLastUpdatableByExtensionSpan(),
                                      traceContext.getLastExporableSpan(),
                                      traceContext.getInitialExportInfo().orElse(null),
                                      traceContext.getTraceState());
  }

  private String traceId;
  private String spanId;
  private final Map<String, String> remoteTraceSerialization = new HashMap<>();

  private InternalSpan lastUpdatableByExtensionSpan;

  public PropagatedTraceContext(String traceId, String spanId, Map<String, String> remoteTraceSerialization,
                                String nameSetBySource,
                                Map<String, String> attributesSetBySource,
                                InternalSpan lastUpdatableByExtensionSpan,
                                InternalSpan lastExportableSpan,
                                InitialExportInfo initialExportInfo,
                                Map<String, String> traceState) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.remoteTraceSerialization.putAll(remoteTraceSerialization);
    this.lastUpdatableByExtensionSpan = lastUpdatableByExtensionSpan;
    this.nameSetBySource = nameSetBySource;
    this.attributesSetBySource = attributesSetBySource;
    this.lastExportableSpan = lastExportableSpan;
    this.initialExportInfo = initialExportInfo;
    this.traceState = traceState;
  }

  @Override
  public String getTraceId() {
    return traceId;
  }

  @Override
  public String getSpanId() {
    return spanId;
  }

  @Override
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  @Override
  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  @Override
  public InternalSpan getLastUpdatableByExtensionSpan() {
    return lastUpdatableByExtensionSpan;
  }

  @Override
  public Map<String, String> getAttributesSetBySource() {
    return attributesSetBySource;
  }

  @Override
  public Optional<String> getNameSetBySource() {
    return ofNullable(nameSetBySource);
  }

  @Override
  public void clearAttributesSetBySource() {
    attributesSetBySource.clear();
  }

  @Override
  public InternalSpan getLastExporableSpan() {
    return lastExportableSpan;
  }

  @Override
  public void setLastExportableSpan(InternalSpan lastExportableSpan) {
    this.lastExportableSpan = lastExportableSpan;
  }

  @Override
  public void setLastUpdatableByExtensionSpan(InternalSpan lastUpdatableByExtensionSpan) {
    this.lastUpdatableByExtensionSpan = lastUpdatableByExtensionSpan;
  }

  @Override
  public Map<String, String> getRemoteTraceSerialization() {
    return remoteTraceSerialization;
  }

  @Override
  public Map<String, String> getTraceState() {
    return traceState;
  }

  @Override
  public void setTraceState(Map<String, String> traceState) {
    this.traceState = traceState;
  }

  @Override
  public void setInitialExportInfo(InitialExportInfo initialExportInfo) {
    this.initialExportInfo = initialExportInfo;
  }

  @Override
  public Optional<InitialExportInfo> getInitialExportInfo() {
    return Optional.ofNullable(initialExportInfo);
  }
}
