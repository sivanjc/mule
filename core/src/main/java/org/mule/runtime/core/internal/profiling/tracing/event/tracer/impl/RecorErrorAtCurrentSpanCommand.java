/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.function.Supplier;

public class RecorErrorAtCurrentSpanCommand implements TracingCommand {

  private final DefaultCoreEventTracer coreEventTracer;
  private final CoreEvent coreEvent;
  private final Supplier<Error> spanError;
  private final boolean isErrorEscapingCurrentSpan;

  public RecorErrorAtCurrentSpanCommand(DefaultCoreEventTracer coreEventTracer, CoreEvent coreEvent, Supplier<Error> spanError,
                                        boolean isErrorEscapingCurrentSpan) {
    this.coreEventTracer = coreEventTracer;
    this.coreEvent = coreEvent;
    this.spanError = spanError;
    this.isErrorEscapingCurrentSpan = isErrorEscapingCurrentSpan;
  }

  @Override
  public void execute() {
    coreEventTracer.doRecordErrorArCurrentSpan(coreEvent, spanError, isErrorEscapingCurrentSpan);
  }
}
