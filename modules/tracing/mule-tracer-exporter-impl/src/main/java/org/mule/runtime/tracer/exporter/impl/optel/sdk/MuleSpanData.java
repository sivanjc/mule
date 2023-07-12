/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static java.util.Collections.emptyList;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporter;

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

/**
 * An implementation of OTEL sdk {@link SpanData}.
 *
 * @since 4.5.0
 */
public class MuleSpanData implements SpanData {

  private final MuleReadableSpan readableSpan;
  private final OpenTelemetrySpanExporter openTelemetrySpanExporter;
  private final Resource resource;
  private final MuleAttributes attributes;
  private String name;

  public MuleSpanData(MuleReadableSpan readableSpan, Resource resource, String artifactId, String artifactType) {
    this.name = readableSpan.getName();
    this.readableSpan = readableSpan;
    this.openTelemetrySpanExporter = readableSpan.getOpenTelemetrySpanExporter();
    this.attributes = new MuleAttributes(openTelemetrySpanExporter, artifactId, artifactType);
    this.resource = resource;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SpanKind getKind() {
    return readableSpan.getKind();
  }

  @Override
  public SpanContext getSpanContext() {
    return readableSpan.getSpanContext();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return readableSpan.getParentSpanContext();
  }

  @Override
  public StatusData getStatus() {
    return openTelemetrySpanExporter.getStatus();
  }

  @Override
  public long getStartEpochNanos() {
    return openTelemetrySpanExporter.getInternalSpan().getDuration().getStart();
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public List<EventData> getEvents() {
    return openTelemetrySpanExporter.getEvents();
  }

  @Override
  public List<LinkData> getLinks() {
    return emptyList();
  }

  @Override
  public long getEndEpochNanos() {
    return openTelemetrySpanExporter.getInternalSpan().getDuration().getEnd();
  }

  @Override
  public boolean hasEnded() {
    return readableSpan.hasEnded();
  }

  @Override
  public int getTotalRecordedEvents() {
    // This is for performance purposes. We know that in the current
    // implementation we only have one error. So we inform this to the open
    // telemetry sdk.
    if (openTelemetrySpanExporter.getInternalSpan().hasErrors()) {
      return 1;
    }

    return 0;
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
    return readableSpan.getInstrumentationLibraryInfo();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return readableSpan.getInstrumentationScopeInfo();
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  public void updateName(String name) {
    this.name = name;
  }

  public void addAttribute(String key, String value) {
    attributes.addAttribute(key, value);
  }
}
