/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static io.opentelemetry.api.trace.SpanContext.getInvalid;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.ANCESTOR_MULE_SPAN_ID;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.TRACE_STATE_KEY;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.getMutableMuleTraceStateFrom;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.TRACE_PARENT;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.extractContextFromTraceParent;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.generateSpanId;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.generateTraceId;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_LIBRARY_INFO;
import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_SCOPE_INFO;

import static io.opentelemetry.api.trace.SpanContext.create;
import static io.opentelemetry.api.trace.TraceFlags.getSampled;
import static io.opentelemetry.api.trace.TraceState.getDefault;

import io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.tracer.api.span.TraceContext;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An implementation of OTEL sdk {@link ReadableSpan}.
 *
 * @since 4.5.0
 */
public class MuleReadableSpan implements ReadableSpan {

  public static MuleReadableSpanBuilder builder() {
    return new MuleReadableSpanBuilder();
  }

  private final boolean enableMuleAncestorIdManagement;

  private final MuleSpanData spanData;


  public MuleReadableSpan(MuleSpanData spanData,
                          boolean enableMuleAncestorIdManagement) {
    this.spanData = spanData;
    this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
  }

  @Override
  public SpanContext getSpanContext() {
    return spanData.getSpanContext();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return spanData.getParentSpanContext();
  }

  @Override
  public String getName() {
    return spanData.getName();
  }

  @Override
  public SpanData toSpanData() {
    return spanData;
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
  public boolean hasEnded() {
    return true;
  }

  @Override
  public long getLatencyNanos() {
    return 0;
  }

  @Override
  public SpanKind getKind() {
    return spanData.getKind();
  }

  @Override
  public <T> T getAttribute(AttributeKey<T> key) {
    return spanData.getAttributes().get(key);
  }

  public boolean isEnableMuleAncestorIdManagement() {
    return enableMuleAncestorIdManagement;
  }

  public static class MuleReadableSpanBuilder {

    private String artifactId;
    private String artifactType;
    private Resource resource;
    private boolean enableMuleAncestorIdManagement;
    private InitialExportInfo initialExportInfo;
    private Supplier<String> nameSupplier;
    private Supplier<SpanKind> kindSupplier;
    private Supplier<StatusData> statusDataSupplier;
    private Supplier<Long> endEpochNanosSupplier;
    private Supplier<Long> startEpochNanosSupplier;
    private Consumer<BiConsumer<String, String>> forEachOperation;
    private Supplier<Integer> attributesSize;
    private TraceContext traceContext;
    private Supplier<List<EventData>> eventsSupplier;

    public MuleReadableSpanBuilder() {}

    public MuleReadableSpanBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public MuleReadableSpanBuilder withArtifactType(String artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public MuleReadableSpanBuilder withResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public MuleReadableSpanBuilder enableMuleAncestorIdManagement(boolean enableMuleAncestorIdManagement) {
      this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
      return this;
    }

    public MuleReadableSpanBuilder withInitialExportInfo(InitialExportInfo initialExportInfo) {
      this.initialExportInfo = initialExportInfo;
      return this;
    }

    public MuleReadableSpanBuilder withNameSupplier(Supplier<String> nameSupplier) {
      this.nameSupplier = nameSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withKindSupplier(Supplier<SpanKind> kindSupplier) {
      this.kindSupplier = kindSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withStatusDataSupplier(Supplier<StatusData> statusDataSupplier) {
      this.statusDataSupplier = statusDataSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withForEachOperation(Consumer<BiConsumer<String, String>> forEachOperation) {
      this.forEachOperation = forEachOperation;
      return this;
    }

    public MuleReadableSpanBuilder withEndEpochNanosSupplier(Supplier<Long> endEpochNanosSupplier) {
      this.endEpochNanosSupplier = endEpochNanosSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withStartEpochNanosSupplier(Supplier<Long> startEpochNanosSupplier) {
      this.startEpochNanosSupplier = startEpochNanosSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withAttributesSize(Supplier<Integer> attributesSize) {
      this.attributesSize = attributesSize;
      return this;
    }

    public MuleReadableSpanBuilder withEventsSupplier(Supplier<List<EventData>> eventsSupplier) {
      this.eventsSupplier = eventsSupplier;
      return this;
    }

    public MuleReadableSpanBuilder withTraceContext(TraceContext traceContext) {
      this.traceContext = traceContext;
      return this;
    }

    public MuleReadableSpan build() {
      MuleAttributes attributes = new MuleAttributes(forEachOperation, artifactId, artifactType, attributesSize);

      String newSpanId = generateSpanId();
      SpanContext parentSpanContext = resolveParentSpanContext(traceContext);
      String traceId = parentSpanContext.getTraceId();

      if (traceId.equals(getInvalid().getTraceId())) {
        traceId = generateTraceId();
      }

      Map<String, String> traceStateAsMap = getMutableTraceStateAsMap(traceContext);

      MuleSpanData muleSpanData = new MuleSpanData(nameSupplier,
                                                   kindSupplier,
                                                   statusDataSupplier,
                                                   endEpochNanosSupplier,
                                                   startEpochNanosSupplier,
                                                   attributes,
                                                   create(traceId, newSpanId, getSampled(),
                                                          getMutableMuleTraceStateFrom(getMutableTraceStateAsMap(traceContext),
                                                                                       enableMuleAncestorIdManagement)),
                                                   create(parentSpanContext.getTraceId(), parentSpanContext.getSpanId(),
                                                          getSampled(),
                                                          getDefault()),
                                                   eventsSupplier,
                                                   resource);
      traceContext.setTraceId(traceId);
      traceContext.setSpanId(newSpanId);

      if (enableMuleAncestorIdManagement) {
        traceStateAsMap.remove(ANCESTOR_MULE_SPAN_ID);
      }
      traceContext.setTraceState(traceStateAsMap);

      return new MuleReadableSpan(muleSpanData, enableMuleAncestorIdManagement);
    }

    private Map<String, String> getMutableTraceStateAsMap(TraceContext traceContext) {
      Map<String, String> traceState = traceContext.getTraceState();
      if (traceContext.getTraceState() != null) {
        return traceState;
      }

      String traceHeader = traceContext.getRemoteTraceSerialization().get(TRACE_STATE_KEY);
      if (!StringUtils.isEmpty(traceHeader)) {
        traceState = new HashMap<>(W3CTraceContextEncoding.decodeTraceState(traceHeader).asMap());
      } else {
        traceState = Collections.emptyMap();
      }

      return traceState;
    }

    private SpanContext resolveParentSpanContext(TraceContext traceContext) {
      if (traceContext.getSpanId() != null && traceContext.getTraceId() != null) {
        return SpanContext.create(traceContext.getTraceId(), traceContext.getSpanId(), getSampled(), getDefault());
      }

      String traceHeaderInRemote = traceContext.getRemoteTraceSerialization().get(TRACE_PARENT);
      return extractContextFromTraceParent(traceHeaderInRemote);
    }
  }
}
