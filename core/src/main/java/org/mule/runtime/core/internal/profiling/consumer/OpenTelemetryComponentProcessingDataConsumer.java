/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableSet.of;
import static java.time.Instant.ofEpochMilli;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;



@RuntimeInternalProfilingDataConsumer
public class OpenTelemetryComponentProcessingDataConsumer
    implements ProfilingDataConsumer<ComponentProcessingStrategyProfilingEventContext> {

  private static SdkTracerProvider sdkTracerProvider;
  private static OpenTelemetry openTelemetry;
  private static Tracer tracer;

  private Map<String, Span> flowSpans = new ConcurrentHashMap<String, Span>();
  private Map<String, Span> operationSpans = new ConcurrentHashMap<String, Span>();
  private Map<String, Span> threadingSpans = new ConcurrentHashMap<>();
  private Map<String, Span> executionSpans = new ConcurrentHashMap<>();

  static {
    ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(OpenTelemetryConfiguration.class.getClassLoader());

    try {
      OpenTelemetryConfiguration.newBuilder()
          .setServiceName("flow-tracer")
          .setAccessToken(
                          "Gf1OypijmmirdLiRs1GxGmug2wQfFqKlyBXx/R2yn88BdHtEzEA0oq9PWqMWtFVRqK9jP/SJPWq2bl0phs1+4Wdl0m+V5T6nvi8CaDo/")
          .install();

      tracer = GlobalOpenTelemetry
          .getTracer("flow-tracer", "1.0.0");
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassloader);
    }
  }

  @Override
  public void onProfilingEvent(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                               ComponentProcessingStrategyProfilingEventContext profilingEventContext) {

    if (profilingEventType == STARTING_FLOW_EXECUTION) {
      // Starts a flow span
      String flowSpanName = getFlowSpanName(profilingEventContext);
      Span rootSpan = tracer.spanBuilder(flowSpanName)
          .setStartTimestamp(ofEpochMilli(profilingEventContext.getTriggerTimestamp())).startSpan();
      addSpanAttributes(profilingEventContext, rootSpan);

      flowSpans.put(flowSpanName, rootSpan);
    }

    if (profilingEventType == PS_SCHEDULING_OPERATION_EXECUTION) {
      String flowSpanName = getFlowSpanName(profilingEventContext);
      Span rootSpan = flowSpans.get(flowSpanName);

      Span operationSpan = tracer
          .spanBuilder(getOperationSpanName(flowSpanName, "/operation/", profilingEventContext))
          .setParent(Context.current().with(rootSpan))
          .setStartTimestamp(ofEpochMilli(profilingEventContext.getTriggerTimestamp()))
          .startSpan();

      addSpanAttributes(profilingEventContext, operationSpan);

      operationSpans
          .put(getOperationSpanName(flowSpanName, "/operation/", profilingEventContext), operationSpan);

      Span threadSwitchSpan = tracer
          .spanBuilder(getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext))
          .setParent(Context.current().with(operationSpan))
          .setStartTimestamp(ofEpochMilli(profilingEventContext.getTriggerTimestamp()))
          .startSpan();

      addSpanAttributes(profilingEventContext, threadSwitchSpan);

      threadingSpans.put(getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext), threadSwitchSpan);

    }

    if (profilingEventType == PS_STARTING_OPERATION_EXECUTION) {
      String flowSpanName = getFlowSpanName(profilingEventContext);
      String childName =
          getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext);
      Span span = threadingSpans.remove(childName);
      span.setAttribute("endThreadName", profilingEventContext.getThreadName());
      span.end(Instant.ofEpochMilli(profilingEventContext.getTriggerTimestamp()));

      String operationSpan =
          getOperationSpanName(flowSpanName, "/operation/", profilingEventContext);
      Span childSpan = operationSpans.get(operationSpan);
      Span threadSwitchSpan = tracer
          .spanBuilder(getOperationSpanName(flowSpanName, "/operation/execution/", profilingEventContext))
          .setParent(Context.current().with(childSpan))
          .setStartTimestamp(ofEpochMilli(profilingEventContext.getTriggerTimestamp()))
          .startSpan();

      addSpanAttributes(profilingEventContext, threadSwitchSpan);

      executionSpans.put(getOperationSpanName(flowSpanName, "/operation/execution/", profilingEventContext), threadSwitchSpan);
    }

    if (profilingEventType == PS_OPERATION_EXECUTED) {
      String flowSpanName = getFlowSpanName(profilingEventContext);
      String childName =
          getOperationSpanName(flowSpanName, "/operation/execution/", profilingEventContext);
      Span span = executionSpans.remove(childName);
      span.setAttribute("endThreadName", profilingEventContext.getThreadName());
      span.end(Instant.ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
      span.addEvent("End Flow Execution", ofEpochMilli(profilingEventContext.getTriggerTimestamp()));

      String operationSpanName =
          getOperationSpanName(flowSpanName, "/operation/", profilingEventContext);

      Span operationSpan = operationSpans.get(operationSpanName);

      Span threadSwitchSpan = tracer
          .spanBuilder(getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext))
          .setParent(Context.current().with(operationSpan))
          .setStartTimestamp(ofEpochMilli(profilingEventContext.getTriggerTimestamp()))
          .startSpan();

      addSpanAttributes(profilingEventContext, threadSwitchSpan);

      threadingSpans.put(getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext), threadSwitchSpan);
    }


    if (profilingEventType == PS_FLOW_MESSAGE_PASSING) {
      String flowSpanName = getFlowSpanName(profilingEventContext);
      String threadSwitchSpan =
          getOperationSpanName(flowSpanName, "/operation/ps/", profilingEventContext);
      Span span = threadingSpans.remove(threadSwitchSpan);
      span.setAttribute("endThreadName", profilingEventContext.getThreadName());
      span.end(Instant.ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
      span.addEvent("End Flow Execution", ofEpochMilli(profilingEventContext.getTriggerTimestamp()));

      String operationSpanName =
          getOperationSpanName(flowSpanName, "/operation/", profilingEventContext);
      Span operationSpan = operationSpans.remove(operationSpanName);
      operationSpan.setAttribute("endThreadName", profilingEventContext.getThreadName());
      operationSpan.end(Instant.ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
      operationSpan.addEvent("End Flow Execution", ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
    }

    if (profilingEventType == FLOW_EXECUTED) {
      String flowSpanName = getFlowSpanName(profilingEventContext);
      Span rootSpan = flowSpans.remove(flowSpanName);
      rootSpan.setAttribute("endThreadName", profilingEventContext.getThreadName());
      rootSpan.end(Instant.ofEpochMilli(profilingEventContext.getTriggerTimestamp()));
      rootSpan.addEvent("End Flow Execution", ofEpochMilli(profilingEventContext.getTriggerTimestamp()));

    }
  }

  private void addSpanAttributes(ComponentProcessingStrategyProfilingEventContext profilingEventContext, Span rootSpan) {
    rootSpan.setAttribute("correlationId", profilingEventContext.getCorrelationId());
    rootSpan.setAttribute("artifactId", profilingEventContext.getArtifactId());
    rootSpan.setAttribute("artifactType", profilingEventContext.getArtifactType());
    rootSpan.setAttribute("location", profilingEventContext.getLocation().get().getLocation());
    rootSpan.setAttribute("startingThreadName", profilingEventContext.getThreadName());
    rootSpan.setAttribute("type",
                          profilingEventContext.getLocation().get().getComponentIdentifier().getIdentifier().getNamespace() + ":"
                              + profilingEventContext.getLocation().get().getComponentIdentifier().getIdentifier().getName());
  }

  private String getOperationSpanName(String flowSpanName, String suffix,
                                      ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    return flowSpanName
        + (profilingEventContext.getLocation().isPresent() ? profilingEventContext.getLocation().get()
            .getLocation() : "")
        + suffix;
  }

  private String getFlowSpanName(ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    return profilingEventContext.getLocation().get().getRootContainerName() + "/"
        + profilingEventContext.getCorrelationId();
  }

  @Override
  public Set<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>> getProfilingEventTypes() {
    return of(PS_SCHEDULING_OPERATION_EXECUTION, PS_STARTING_OPERATION_EXECUTION, PS_OPERATION_EXECUTED,
              PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
              FLOW_EXECUTED);
  }

  @Override
  public Predicate<ComponentProcessingStrategyProfilingEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }
}
