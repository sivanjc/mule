/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;

public class AddCurrentSpanAttributeCommand implements TracingCommand {

  private final DefaultCoreEventTracer coreEventTracer;
  private final CoreEvent coreEvent;
  private final String key;
  private final String value;

  public AddCurrentSpanAttributeCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent, String key, String value) {
    this.coreEventTracer = coreEventTracer;
    this.coreEvent = coreEvent;
    this.key = key;
    this.value = value;
  }

  @Override
  public void execute() {
    coreEventTracer.executeAddCurrentSpanAttribute(coreEvent, key, value);
  }
}
