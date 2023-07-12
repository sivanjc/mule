/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.getMutableMuleTraceStateFrom;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTIONS_HAVE_BEEN_RECORDED;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_ESCAPED_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_EVENT_NAME;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_MESSAGE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_STACK_TRACE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_TYPE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.SPAN_KIND;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.STATUS;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.extractContextFromTraceParent;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.getDistributedTraceContext;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.MuleReadableSpan.muleReadableSpanBuilderFor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.SpanContext.getInvalid;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.sdk.trace.data.StatusData.unset;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.impl.optel.sdk.MuleReadableSpan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.StatusData;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter {

  public static final String TRACE_PARENT_KEY = "traceparent";
  private final boolean isRootSpan;
  private final boolean isPolicySpan;
  private final InternalSpan internalSpan;
  private final SpanProcessor spanProcessor;
  private final boolean enableMuleAncestorIdManagement;
  private SpanKind spanKind = INTERNAL;
  private StatusData statusData = unset();
  private List<EventData> errorEvents = emptyList();
  private OpenTelemetrySpanExporter rootSpanExporter = this;
  private String endThreadNameValue;
  private MuleReadableSpan readableSpan;

  private OpenTelemetryExportTraceContext openTelemetryExportTraceContextInformation;

  public OpenTelemetrySpanExporter(InternalSpan internalSpan,
                                   InitialSpanInfo initialSpanInfo,
                                   String artifactId,
                                   String artifactType,
                                   SpanProcessor spanProcessor,
                                   boolean enableMuleAncestorIdManagement,
                                   Resource resource) {
    requireNonNull(internalSpan);
    requireNonNull(initialSpanInfo);
    requireNonNull(artifactId);
    requireNonNull(artifactType);
    requireNonNull(spanProcessor);
    requireNonNull(resource);
    this.internalSpan = internalSpan;
    this.isRootSpan = initialSpanInfo.isRootSpan();
    this.isPolicySpan = initialSpanInfo.isPolicySpan();
    this.spanProcessor = spanProcessor;
    this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
    this.openTelemetryExportTraceContextInformation = new OpenTelemetryExportTraceContext(enableMuleAncestorIdManagement);
    this.readableSpan = muleReadableSpanBuilderFor(this)
        .withResource(resource)
        .withArtifactId(artifactId)
        .withArtifactType(artifactType)
        .enableMuleAncestorIdManagement(enableMuleAncestorIdManagement)
        .withInitialExportInfo(initialSpanInfo.getInitialExportInfo())
        .build();
    this.openTelemetryExportTraceContextInformation.setSpanContext(readableSpan.getSpanContext());
  }

  @Override
  public void export() {
    endThreadNameValue = Thread.currentThread().getName();
    spanProcessor.onEnd(readableSpan);
  }

  @Override
  public void updateNameForExport(String newName) {
    openTelemetryExportTraceContextInformation.getParentExportableReadableSpan().ifPresent(rs -> rs.updateName(newName));
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return getDistributedTraceContext(this, enableMuleAncestorIdManagement);
  }

  @Override
  public void setRootName(String rootName) {
    openTelemetryExportTraceContextInformation.setRootName(rootName);
  }

  @Override
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    openTelemetryExportTraceContextInformation.addRootAttribute(rootAttributeKey, rootAttributeValue);
  }

  @Override
  public void updateParentSpanFrom(Map<String, String> serializeAsMap) {
    openTelemetryExportTraceContextInformation
        .setTraceState(getMutableMuleTraceStateFrom(serializeAsMap, enableMuleAncestorIdManagement));
    readableSpan.setParentSpanContext(extractContextFromTraceParent(serializeAsMap.get(TRACE_PARENT_KEY)));
  }

  @Override
  public SpanIdentifier getSpanIdentifier() {
    return new OpentelemetrySpanIdentifier(readableSpan.getSpanContext().getSpanId(), readableSpan.getSpanContext().getTraceId());
  }

  @Override
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter childOpenTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;

      childOpenTelemetrySpanExporter.readableSpan
          .setParentSpanContext(openTelemetryExportTraceContextInformation.getSpanContext());

      openTelemetryExportTraceContextInformation.getTraceState()
          .propagateRemoteContext(childOpenTelemetrySpanExporter.openTelemetryExportTraceContextInformation.getTraceState());

      if (childOpenTelemetrySpanExporter.isRootSpan) {
        openTelemetryExportTraceContextInformation.getRootName().ifPresent(readableSpan::updateName);
        openTelemetryExportTraceContextInformation.getRootAttributes().forEach(readableSpan::addAttribute);
      } else {
        childOpenTelemetrySpanExporter.openTelemetryExportTraceContextInformation
            .setRootName(openTelemetryExportTraceContextInformation.getRootName().orElse(null));
        childOpenTelemetrySpanExporter.openTelemetryExportTraceContextInformation.getRootAttributes()
            .forEach(openTelemetryExportTraceContextInformation::addRootAttribute);
      }

      // If it isn't exportable propagate the traceId and spanId
      if (!childOpenTelemetrySpanExporter.readableSpan.isExportable()) {
        childOpenTelemetrySpanExporter.openTelemetryExportTraceContextInformation = openTelemetryExportTraceContextInformation;
      }
    }
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }

  public String getName() {
    return internalSpan.getName();
  }

  @Override
  public void onAdditionalAttribute(String key, String value) {
    if (key.equals(SPAN_KIND)) {
      rootSpanExporter.spanKind = SpanKind.valueOf(value);
    } else if (key.equals(STATUS)) {
      StatusCode statusCode = StatusCode.valueOf(value);
      rootSpanExporter.statusData = StatusData.create(statusCode, null);
    } else if (isPolicySpan && !rootSpanExporter.equals(this)) {
      rootSpanExporter.internalSpan.addAttribute(key, value);
    }
  }

  public StatusData getStatus() {
    return statusData;
  }

  public List<EventData> getEvents() {
    return errorEvents;
  }

  @Override
  public void onError(InternalSpanError spanError) {
    statusData = StatusData.create(ERROR, EXCEPTIONS_HAVE_BEEN_RECORDED);
    Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                    EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                    EXCEPTION_STACK_TRACE_KEY,
                                    getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                    EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());

    errorEvents = singletonList(EventData.create(System.currentTimeMillis(), EXCEPTION_EVENT_NAME, errorAttributes));
  }

  public MutableMuleTraceState getTraceState() {
    return openTelemetryExportTraceContextInformation.getTraceState();
  }

  public SpanKind getKind() {
    return spanKind;
  }

  public String getThreadEndName() {
    return endThreadNameValue;
  }

  public String getSpanId() {
    return readableSpan.getSpanContext().getSpanId();
  }

  public String getTraceId() {
    return readableSpan.getSpanContext().getTraceId();
  }

}
