/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.factory;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.impl.clock.Clock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.tracer.impl.clock.Clock.getDefault;
import static org.mule.runtime.tracer.impl.span.factory.LazySpanIdUtils.generateSpanId;
import static org.mule.runtime.tracer.impl.span.factory.LazySpanIdUtils.generateTraceId;

public class LazyExecutionSpan implements InternalSpan {

  private final InitialSpanInfo initialSpanInfo;
  private final InternalSpan parentInternalSpan;
  private final SpanExporterFactory spanExporterFactory;
  private final String spanId;
  private final String traceId;
  private final long startTime;
  private long endTime;

  // There is only one error in execution.
  private InternalSpanError error;
  private String updatedName;
  private Map<String, String> attributes = new HashMap<>();

  public static final String THREAD_END_NAME = "thread.end.name";


  public LazyExecutionSpan(InitialSpanInfo initialSpanInfo, InternalSpan parentInternalSpan,
                           SpanExporterFactory spanExporterFactory) {
    this.initialSpanInfo = initialSpanInfo;
    this.parentInternalSpan = parentInternalSpan;
    this.spanExporterFactory = spanExporterFactory;
    this.spanId = generateSpanId();
    this.traceId = generateTraceId(parentInternalSpan);
    this.startTime = Clock.getDefault().now();
  }

  public String getTraceId() {
    return traceId;
  }

  @Override
  public void end() {
    SpanExporter spanExporter = spanExporterFactory.getSpanExporter(this, initialSpanInfo);
    this.attributes.put(THREAD_END_NAME, currentThread().getName());
    this.endTime = getDefault().now();
    spanExporter.export();

  }

  @Override
  public void addAttribute(String key, String value) {
    attributes.put(key, value);
  }

  @Override
  public Optional<String> getAttribute(String key) {
    return ofNullable(attributes.get(key));
  }

  @Override
  public void addError(InternalSpanError error) {
    this.error = error;
  }

  @Override
  public void updateName(String updatedName) {
    this.updatedName = updatedName;
  }

  @Override
  public SpanExporter getSpanExporter() {
    return spanExporterFactory.getSpanExporter(this, initialSpanInfo);
  }

  @Override
  public Map<String, String> getAttributes() {
    return attributes;
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return LazySpanIdUtils.getContext(this);
  }

  @Override
  public Span getParent() {
    return parentInternalSpan;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return new LazyExecutionSpanIdentifier(getSpanId());
  }

  public String getSpanId() {
    return spanId;
  }

  @Override
  public String getName() {
    if (updatedName != null) {
      return updatedName;
    }

    return initialSpanInfo.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return new DefaultSpanDuration(startTime, endTime);
  }

  @Override
  public List<SpanError> getErrors() {
    return Collections.singletonList(error);
  }

  @Override
  public boolean hasErrors() {
    return false;
  }

  private class LazyExecutionSpanIdentifier implements SpanIdentifier {

    private final String id;

    public LazyExecutionSpanIdentifier(String id) {
      this.id = id;
    }

    @Override
    public String getId() {
      return id;
    }
  }

  /**
   * An default implementation for a {@link SpanDuration}
   */
  private static class DefaultSpanDuration implements SpanDuration {

    private final Long startTime;
    private final Long endTime;

    public DefaultSpanDuration(Long startTime, Long endTime) {
      this.startTime = startTime;
      this.endTime = endTime;
    }

    @Override
    public Long getStart() {
      return startTime;
    }

    @Override
    public Long getEnd() {
      return endTime;
    }
  }
}
