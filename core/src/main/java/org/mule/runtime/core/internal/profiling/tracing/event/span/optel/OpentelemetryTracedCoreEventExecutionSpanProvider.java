/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import static java.lang.System.currentTimeMillis;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCoreEventSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCurrentContextSpan;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.optel.OpenTelemetryResourcesProvider.getOpentelemetryTracer;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.InternalSpan;
import org.mule.runtime.core.internal.profiling.OpentelemetryExecutionSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventExecutionSpanProvider;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;

import io.opentelemetry.api.trace.Tracer;

/**
 * A {@link CoreEventExecutionSpanProvider} that provides open telemetry traced
 * {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * @since 4.5.0
 */
public class OpentelemetryTracedCoreEventExecutionSpanProvider implements CoreEventExecutionSpanProvider {

  private final Tracer openTelemetryTracer = getOpentelemetryTracer();

  private static final CoreEventSpanCustomizer defaultCoreEventSpanCustomizer = new DefaultEventSpanCustomizer();

  @Override
  public InternalSpan getSpan(CoreEvent event, Component component, MuleConfiguration muleConfiguration) {
    return getOpentelemetrySpan(component,
                                muleConfiguration,
                                event.getContext(),
                                defaultCoreEventSpanCustomizer.getName(event, component))
                                    .startOpentelemetrySpan();
  }

  @Override
  public InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                              CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return getOpentelemetrySpan(component, muleConfiguration, coreEvent.getContext(),
                                coreEventSpanCustomizer.getName(coreEvent, component))
                                    .startOpentelemetrySpan();
  }

  private OpentelemetryExecutionSpan getOpentelemetrySpan(Component component, MuleConfiguration muleConfiguration,
                                                          EventContext eventContext, String name) {
    return new OpentelemetryExecutionSpan(new ExecutionSpan(name,
                                                            componentSpanIdentifierFrom(muleConfiguration.getId(),
                                                                                        component.getLocation(),
                                                                                        eventContext.getCorrelationId()),
                                                            currentTimeMillis(),
                                                            null,
                                                            getCurrentContextSpan(eventContext).orElse(null)),
                                          eventContext, openTelemetryTracer);
  }

  private static final class DefaultEventSpanCustomizer implements CoreEventSpanCustomizer {

    @Override
    public String getName(CoreEvent coreEvent, Component component) {
      return getCoreEventSpanName(component.getIdentifier());
    }
  }
}
