/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;

public class SetCurrentSpanNameCommand implements TracingCommand {

  private final DefaultCoreEventTracer coreEventTracer;
  private final CoreEvent coreEvent;
  private final String name;

  public SetCurrentSpanNameCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent, String name) {
    this.coreEventTracer = coreEventTracer;
    this.coreEvent = coreEvent;
    this.name = name;
  }

  @Override
  public void execute() {
    coreEventTracer.executeSetCurrentSpanName(coreEvent, name);
  }
}
