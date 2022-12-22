/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import io.grpc.Internal;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import io.opentelemetry.sdk.trace.internal.JcTools;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;
import org.mule.runtime.tracer.impl.exporter.optel.span.provider.MuleOpenTelemetrySpanProvider;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.Queue;

import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.builder;

public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory {

  private static final TextMapGetter<Map<String, String>> OPEN_TELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  private final Worker worker;
  @Inject
  MuleContext muleContext;

  public OpenTelemetrySpanExporterFactory() {
    this.worker =
        new Worker(
                   JcTools.newFixedSizeQueue(1000));
    Thread workerThread = new DaemonThreadFactory("enqueuer").newThread(worker);
    workerThread.start();
  }

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, InitialSpanInfo initialExportInfo) {
    return new OpenTelemetryLazyExporter(internalSpan, initialExportInfo);
  }

  @Override
  public SpanSnifferManager getSpanExporterManager() {
    return new OpenTelemetrySpanExporterManager();
  }

  private static class OpenTelemetrySpanExporterManager implements SpanSnifferManager {

    @Override
    public ExportedSpanSniffer getExportedSpanSniffer() {
      return OpenTelemetryResources.getNewExportedSpanCapturer();
    }
  }


  private class OpenTelemetryLazyExporter implements OptelExporter, SpanExporter {

    private final InternalSpan internalSpan;
    private final InitialSpanInfo initialExportInfo;

    public OpenTelemetryLazyExporter(InternalSpan internalSpan,
                                     InitialSpanInfo initialExportInfo) {
      this.internalSpan = internalSpan;
      this.initialExportInfo = initialExportInfo;
    }

    @Override
    public void export() {
      if (worker.queue.size() < 1000) {
        worker.queue.offer(new Pair(internalSpan, initialExportInfo));
      }

    }

    @Override
    public void updateNameForExport(String newName) {

    }

    @Override
    public Map<String, String> exportedSpanAsMap() {
      return internalSpan.serializeAsMap();
    }

    @Override
    public InternalSpan getInternalSpan() {
      return InternalSpan.getAsInternalSpan(internalSpan);
    }

    @Override
    public Context getOpenTelemetrySpan() {
      return W3CTraceContextPropagator.getInstance().extract(Context.current(), internalSpan.serializeAsMap(),
                                                             OPEN_TELEMETRY_SPAN_GETTER);
    }
  }


  private class Worker implements Runnable {

    private final Queue<Pair<InternalSpan, InitialSpanInfo>> queue;

    public Worker(Queue<Pair<InternalSpan, InitialSpanInfo>> queue) {
      this.queue = queue;
    }

    @Override
    public void run() {
      while (true) {
        if (queue.isEmpty()) {
          try {
            Thread.sleep(1000);
            continue;
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        Pair<InternalSpan, InitialSpanInfo> span = queue.poll();
        SpanExporter spanExporter = builder()
            .withStartSpanInfo(span.getSecond())
            .withArtifactId(muleContext.getConfiguration().getId())
            .withArtifactType(muleContext.getArtifactType().getAsString())
            .withInternalSpan(span.getFirst())
            .build();

        spanExporter.export();
      }
    }
  }

  /**
   * An Internal {@link TextMapGetter} to retrieve the remote span context.
   *
   * This is used to resolve a remote OpTel Span propagated through W3C Trace Context.
   */
  private static class MuleOpenTelemetryRemoteContextGetter implements TextMapGetter<Map<String, String>> {

    @Override
    public Iterable<String> keys(Map<String, String> stringStringMap) {
      return stringStringMap.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, String> stringStringMap, @Nullable String key) {
      if (stringStringMap == null) {
        return null;
      }

      return stringStringMap.get(key);
    }
  }
}
