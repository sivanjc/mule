/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTIONS_HAVE_BEEN_RECORDED;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_ESCAPED_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_EVENT_NAME;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_MESSAGE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_STACK_TRACE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_TYPE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.SPAN_KIND;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.STATUS;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.getDistributedTraceContext;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.impl.optel.sdk.MuleReadableSpan;

import java.util.List;
import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter {

  private final MuleReadableSpan readableSpan;

  private final SpanProcessor spanProcessor;

  public OpenTelemetrySpanExporter(SpanProcessor spanProcessor,
                                   MuleReadableSpan readableSpan) {
    this.readableSpan = readableSpan;
    this.spanProcessor = spanProcessor;
  }

  public static OpenTelemetrySpanExporterBuilder builder(InternalSpan internalSpan) {
    return new OpenTelemetrySpanExporterBuilder(internalSpan);
  }

  @Override
  public void export() {
    spanProcessor.onEnd(readableSpan);
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return getDistributedTraceContext(readableSpan);
  }

  public static class OpenTelemetrySpanExporterBuilder {

    private final InternalSpan internalSpan;
    private InitialSpanInfo initialSpanInfo;
    private String artifactId;
    private String artifactType;
    private Resource resource;
    private boolean addMuleAncestorSpanId;
    private SpanProcessor spanProcesor;

    private OpenTelemetrySpanExporterBuilder(InternalSpan internalSpan) {
      this.internalSpan = internalSpan;
    }

    public OpenTelemetrySpanExporterBuilder using(InitialSpanInfo initialSpanInfo) {
      this.initialSpanInfo = initialSpanInfo;
      return this;
    }

    public OpenTelemetrySpanExporterBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public OpenTelemetrySpanExporterBuilder withArtifactType(String artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public OpenTelemetrySpanExporterBuilder withResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public OpenTelemetrySpanExporterBuilder withAddMuleAncestorSpanId(boolean addMuleAncestorSpanId) {
      this.addMuleAncestorSpanId = addMuleAncestorSpanId;
      return this;
    }

    public OpenTelemetrySpanExporterBuilder withSpanProcessor(SpanProcessor spanProcessor) {
      this.spanProcesor = spanProcessor;
      return this;
    }

    public SpanExporter build() {
      requireNonNull(internalSpan);
      requireNonNull(artifactId);
      requireNonNull(artifactType);
      requireNonNull(initialSpanInfo);
      requireNonNull(resource);

      internalSpan.getTraceContext().getInitialExportInfo()
          .ifPresent(parentInitialExportInfo -> initialSpanInfo.getInitialExportInfo()
              .propagateInitialExportInfo(parentInitialExportInfo));
      internalSpan.getTraceContext().setInitialExportInfo(initialSpanInfo.getInitialExportInfo());

      if (!initialSpanInfo.isPolicySpan()) {
        internalSpan.getTraceContext().setLastUpdatableByExtensionSpan(internalSpan);
      }

      // If the span is not exportable or the parent indicates that this should not be exported.
      // we propagate the context with the corresponding export until.
      if (!initialSpanInfo.getInitialExportInfo().isExportable()) {
        return NOOP_EXPORTER;
      }

      internalSpan.getTraceContext().setLastExportableSpan(internalSpan);

      MuleReadableSpan muleReadableSpan = MuleReadableSpan.builder()
          .withResource(resource)
          .withArtifactId(artifactId)
          .withArtifactType(artifactType)
          .enableMuleAncestorIdManagement(addMuleAncestorSpanId)
          .withInitialExportInfo(initialSpanInfo.getInitialExportInfo())
          .withNameSupplier(initialSpanInfo::getName)
          .withAttributesSize(() -> getExportableAttributesSize(internalSpan))
          .withForEachOperation(internalSpan::forEachAttribute)
          .withStartEpochNanosSupplier(() -> internalSpan.getDuration().getStart())
          .withEndEpochNanosSupplier(() -> internalSpan.getDuration().getEnd())
          .withKindSupplier(() -> getSpanKind(internalSpan))
          .withEventsSupplier(() -> getErrors(internalSpan))
          .withStatusDataSupplier(() -> getStatusSupplier(internalSpan))
          .withTraceContext(internalSpan.getTraceContext())
          .build();

      return new OpenTelemetrySpanExporter(spanProcesor, muleReadableSpan);
    }

    private Integer getExportableAttributesSize(InternalSpan internalSpan) {
      return internalSpan.getAttributesCount() - minusAttribues(internalSpan);
    }

    private int minusAttribues(InternalSpan internalSpan) {
      final Integer[] count = new Integer[1];
      count[0] = 0;
      internalSpan.forEachAttribute((key, value) -> {
        if (key.equals(STATUS) || key.equals(SPAN_KIND)) {
          count[0] = count[0] + 1;
        }
      });

      return count[0];
    }

    private StatusData getStatusSupplier(InternalSpan internalSpan) {
      StatusData[] statusData = new StatusData[1];

      statusData[0] = StatusData.unset();

      if (internalSpan.hasErrors()) {
        statusData[0] = StatusData.create(ERROR, EXCEPTIONS_HAVE_BEEN_RECORDED);
      }

      internalSpan.forEachAttribute((key, value) -> {
        if (key.equals(STATUS)) {
          StatusCode statusCode = StatusCode.valueOf(value);
          statusData[0] = StatusData.create(statusCode, null);;
        }
      });


      return statusData[0];
    }

    private List<EventData> getErrors(InternalSpan internalSpan) {
      List<SpanError> spanErrors = internalSpan.getErrors();
      if (!spanErrors.isEmpty()) {
        SpanError spanError = spanErrors.get(0);
        Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                        EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                        EXCEPTION_STACK_TRACE_KEY,
                                        getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                        EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());

        return singletonList(EventData.create(System.currentTimeMillis(), EXCEPTION_EVENT_NAME, errorAttributes));
      }

      return emptyList();
    }

    private SpanKind getSpanKind(InternalSpan internalSpan) {
      SpanKind[] spanKind = new SpanKind[1];
      spanKind[0] = INTERNAL;
      internalSpan.forEachAttribute((key, value) -> {
        if (key.equals(SPAN_KIND)) {
          spanKind[0] = SpanKind.valueOf(value);
        }
      });

      return spanKind[0];
    }
  }
}
