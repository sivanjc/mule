/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.TraceContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RootTraceContext implements TraceContext {

  public static TraceContext from(Map<String, String> mapSerialization) {
    return new RootTraceContext(mapSerialization);
  }

  private final Map<String, String> remoteTraceSerialization;
  private Map<String, String> attributesSetBySource = new HashMap<>();
  private String nameSetBySource;

  public RootTraceContext(Map<String, String> remoteTraceSerialization) {
    this.remoteTraceSerialization = remoteTraceSerialization;
    this.nameSetBySource = null;
  }

  @Override
  public String getTraceId() {
    return null;
  }

  @Override
  public String getSpanId() {
    return null;
  }

  @Override
  public InternalSpan getLastUpdatableByExtensionSpan() {
    return null;
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
  public Map<String, String> getRemoteTraceSerialization() {
    return remoteTraceSerialization;
  }

  @Override
  public InternalSpan getLastExporableSpan() {
    return null;
  }

  @Override
  public void setNameSetBySource(String nameSetBySource) {
    this.nameSetBySource = nameSetBySource;
  }

  @Override
  public void setAttributeSetBySource(String key, String value) {
    attributesSetBySource.put(key, value);
  }
}
