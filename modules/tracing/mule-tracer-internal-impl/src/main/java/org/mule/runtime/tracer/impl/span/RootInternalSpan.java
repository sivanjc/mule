/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span;

import static org.mule.runtime.api.profiling.tracing.SpanIdentifier.INVALID_SPAN_IDENTIFIER;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.impl.context.DeferredEndSpanWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class RootInternalSpan implements InternalSpan {

  protected boolean managedChildSpan;

  public static final String ROOT_SPAN = "root";

  private String name = ROOT_SPAN;
  private Map<String, String> attributes = new HashMap<>();

  // This is a managed span that will not end.
  private DeferredEndSpanWrapper managedSpan;

  @Override
  public Span getParent() {
    return null;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return INVALID_SPAN_IDENTIFIER;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SpanDuration getDuration() {
    return null;
  }

  @Override
  public List<SpanError> getErrors() {
    return null;
  }

  @Override
  public boolean hasErrors() {
    return false;
  }

  @Override
  public void end() {
    if (managedSpan != null) {
      managedSpan.doEndOriginalSpan();
    }
  }

  @Override
  public void end(long endTime) {
    end();
  }

  @Override
  public void addError(InternalSpanError error) {
    // Nothing to do.
  }

  @Override
  public void updateName(String name) {
    this.name = name;
    getTraceContext().setNameSetBySource(name);
  }

  @Override
  public int getAttributesCount() {
    return attributes.size();
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return emptyMap();
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    attributes.forEach(biConsumer);
  }

  @Override
  public void addAttribute(String key, String value) {
    if (managedSpan != null) {
      managedSpan.addAttribute(key, value);
    }
    attributes.put(key, value);
  }
}
