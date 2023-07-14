/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_LIBRARY_INFO;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_SCOPE_INFO;

import static java.util.Collections.emptyList;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.List;
import java.util.function.Supplier;

/**
 * An implementation of OTEL sdk {@link SpanData}.
 *
 * @since 4.5.0
 */
public class MuleSpanData implements SpanData {

  private final Resource resource;
  private final MuleAttributes attributes;
  private final Supplier<String> nameSupplier;
  private final Supplier<SpanKind> kindSupplier;
  private final SpanContext spanContext;
  private final SpanContext parentSpanContext;
  private final Supplier<StatusData> statusDataSupplier;
  private final Supplier<List<EventData>> eventsSupplier;
  private final Supplier<Long> endEpochNanosSupplier;
  private final Supplier<Long> startEpochNanosSupplier;

  public MuleSpanData(Supplier<String> nameSupplier,
                      Supplier<SpanKind> kindSupplier,
                      Supplier<StatusData> statusDataSupplier,
                      Supplier<Long> endEpochNanosSupplier,
                      Supplier<Long> startEpochNanosSupplier,
                      MuleAttributes attributes,
                      SpanContext spanContext,
                      SpanContext parentSpanContext,
                      Supplier<List<EventData>> eventsSupplier,
                      Resource resource) {
    this.nameSupplier = nameSupplier;
    this.kindSupplier = kindSupplier;
    this.statusDataSupplier = statusDataSupplier;
    this.endEpochNanosSupplier = endEpochNanosSupplier;
    this.startEpochNanosSupplier = startEpochNanosSupplier;
    this.eventsSupplier = eventsSupplier;
    this.attributes = attributes;
    this.spanContext = spanContext;
    this.parentSpanContext = parentSpanContext;
    this.resource = resource;
  }

  @Override
  public String getName() {
    return nameSupplier.get();
  }

  @Override
  public SpanKind getKind() {
    return kindSupplier.get();
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public SpanContext getParentSpanContext() {
    return parentSpanContext;
  }

  @Override
  public StatusData getStatus() {
    return statusDataSupplier.get();
  }

  @Override
  public long getStartEpochNanos() {
    return startEpochNanosSupplier.get();
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public List<EventData> getEvents() {
    return eventsSupplier.get();
  }

  @Override
  public List<LinkData> getLinks() {
    return emptyList();
  }

  @Override
  public long getEndEpochNanos() {
    return endEpochNanosSupplier.get();
  }

  @Override
  public boolean hasEnded() {
    return true;
  }

  @Override
  public int getTotalRecordedEvents() {
    return getEvents().size();
  }

  @Override
  public int getTotalRecordedLinks() {
    return 0;
  }

  @Override
  public int getTotalAttributeCount() {
    return attributes.size();
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
  public Resource getResource() {
    return resource;
  }
}
