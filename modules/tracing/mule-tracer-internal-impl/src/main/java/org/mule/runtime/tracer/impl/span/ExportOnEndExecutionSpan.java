/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.tracer.impl.clock.Clock.getDefault;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.TraceContext;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link Span} that represents the trace corresponding to the execution of mule flow or component.
 *
 * @since 4.5.0
 */
public class ExportOnEndExecutionSpan implements InternalSpan {

  public static final String THREAD_END_NAME = "thread.end.name";
  private ExportableInitialSpanInfo initialSpanInfo;
  private final SpanExporter spanExporter;
  private SpanError lastError;
  private final InternalSpan parent;
  private final Long startTime;
  private Long endTime;
  private final Map<String, String> additionalAttributes = new HashMap<>();
  private TraceContext traceContext;
  private String name;

  private ExportOnEndExecutionSpan(SpanExporterFactory spanExporterFactory, InitialSpanInfo initialSpanInfo, Long startTime,
                                   InternalSpan parent) {
    this.startTime = startTime;
    this.parent = parent;
    this.traceContext = PropagatedTraceContext.from(parent.getTraceContext());
    this.name = initialSpanInfo.getName();
    String exportableName;
    if (initialSpanInfo.isRootSpan()) {
      exportableName = traceContext.getNameSetBySource().orElse(initialSpanInfo.getName());
      this.additionalAttributes.putAll(traceContext.getAttributesSetBySource());
      this.traceContext.setNameSetBySource(null);
      this.traceContext.clearAttributesSetBySource();
    } else {
      exportableName = initialSpanInfo.getName();
    }
    this.initialSpanInfo = new ExportableInitialSpanInfo(initialSpanInfo, exportableName);
    this.spanExporter = spanExporterFactory.getSpanExporter(this, this.initialSpanInfo);
  }

  public static InternalSpan createExportOnEndExecutionSpan(SpanExporterFactory spanExporterFactory, InternalSpan parentSpan,
                                                            InitialSpanInfo initialSpanInfo) {
    requireNonNull(spanExporterFactory);
    requireNonNull(initialSpanInfo);
    return new ExportOnEndExecutionSpan(spanExporterFactory, initialSpanInfo,
                                        getDefault().now(),
                                        parentSpan);
  }

  @Override
  public TraceContext getTraceContext() {
    return traceContext;
  }

  @Override
  public void setTraceContext(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @Override
  public void updateRootName(String name) {
    InternalSpan lastSpan = traceContext.getLastUpdatableByExtensionSpan();
    if (lastSpan != null) {
      lastSpan.updateRootName(name);
    }
  }

  @Override
  public void end() {
    end(getDefault().now());
  }

  @Override
  public void end(long endTime) {
    this.endTime = endTime;
    additionalAttributes.put(THREAD_END_NAME, Thread.currentThread().getName());
    this.spanExporter.export();
  }

  @Override
  public void addError(InternalSpanError error) {
    this.lastError = error;
  }

  @Override
  public void updateName(String name) {
    InternalSpan lastSpan = traceContext.getLastUpdatableByExtensionSpan();
    if (lastSpan != null) {
      if (lastSpan != this) {
        lastSpan.updateName(name);
      } else {
        initialSpanInfo.updateName(name);
      }
    }
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    initialSpanInfo.forEachAttribute(biConsumer);
    if (!additionalAttributes.isEmpty()) {
      additionalAttributes.forEach(biConsumer);
    }
  }

  @Override
  public Map<String, String> serializeAsMap() {
    InternalSpan lastSpan = traceContext.getLastExporableSpan();
    if (lastSpan != null) {
      if (lastSpan != this) {
        return lastSpan.serializeAsMap();
      }

    }
    return spanExporter.exportedSpanAsMap();
  }

  @Override
  public boolean hasErrors() {
    return lastError != null;
  }

  @Override
  public Span getParent() {
    return parent;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SpanDuration getDuration() {
    return new DefaultSpanDuration(startTime, endTime);
  }

  @Override
  public List<SpanError> getErrors() {
    if (lastError != null) {
      return singletonList(lastError);
    }

    return emptyList();
  }

  @Override
  public int getAttributesCount() {
    return initialSpanInfo.getInitialAttributesCount() + additionalAttributes.size();
  }

  @Override
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    InternalSpan lastSpan = traceContext.getLastUpdatableByExtensionSpan();
    if (lastSpan != null) {
      lastSpan.setRootAttribute(rootAttributeKey, rootAttributeValue);
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

  @Override
  public void addAttribute(String key, String value) {
    InternalSpan lastSpan = traceContext.getLastUpdatableByExtensionSpan();
    if (lastSpan != null) {
      if (lastSpan != this) {
        lastSpan.addAttribute(key, value);
      } else {
        additionalAttributes.put(key, value);
      }
    }

  }

}

