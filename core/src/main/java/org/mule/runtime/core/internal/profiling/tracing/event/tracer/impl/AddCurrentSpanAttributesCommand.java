/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

public class AddCurrentSpanAttributesCommand implements TracingCommand {

  private final DefaultCoreEventTracer coreEventTracer;
  private final CoreEvent coreEvent;
  private final Map<String, String> attributes;

  public AddCurrentSpanAttributesCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent,
                                         Map<String, String> attributes) {
    this.coreEventTracer = coreEventTracer;
    this.coreEvent = coreEvent;
    this.attributes = attributes;
  }

  @Override
  public void execute() {
    coreEventTracer.executeAddCurrentSpanAttributes(coreEvent, attributes);
  }
}
