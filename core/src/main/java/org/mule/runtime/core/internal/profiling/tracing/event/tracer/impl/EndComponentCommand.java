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

public class EndComponentCommand implements TracingCommand {

  private final CoreEvent coreEvent;
  private final TracingCondition tracingCondition;
  private final DefaultCoreEventTracer coreEventTracer;

  public EndComponentCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent, TracingCondition tracingCondition) {
    this.coreEvent = coreEvent;
    this.tracingCondition = tracingCondition;
    this.coreEventTracer = coreEventTracer;
  }

  @Override
  public void execute() {
    coreEventTracer.doEndCurrentSpan(coreEvent, tracingCondition);
  }
}
