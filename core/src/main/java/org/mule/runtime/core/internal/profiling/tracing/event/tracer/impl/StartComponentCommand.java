/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

public class StartComponentCommand implements TracingCommand {

  private final DefaultCoreEventTracer coreEventTracer;
  private final SpanCustomizationInfo spanCustomizationInfo;
  private final TracingCondition tracingCondition;
  private final CoreEvent coreEvent;

  public StartComponentCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent,
                               SpanCustomizationInfo spanCustomizationInfo,
                               TracingCondition tracingCondition) {
    this.coreEventTracer = coreEventTracer;
    this.spanCustomizationInfo = spanCustomizationInfo;
    this.tracingCondition = tracingCondition;
    this.coreEvent = coreEvent;
  }

  @Override
  public void execute() {
    coreEventTracer.doStartComponentSpan(coreEvent, spanCustomizationInfo, tracingCondition);
  }
}
