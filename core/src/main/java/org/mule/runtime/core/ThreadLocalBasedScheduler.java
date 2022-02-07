/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

public class ThreadLocalBasedScheduler
    implements reactor.core.scheduler.Scheduler {


  private ThreadLocal<CoreEvent> localCoreEvent = new ThreadLocal<>();
  private Scheduler delegate;


  @Override
  public Disposable schedule(Runnable runnable) {
    return null;
  }

  @Override
  public Worker createWorker() {
    return null;
  }

  public void setLocalCoreEvent(CoreEvent coreEvent) {
    this.localCoreEvent.set(coreEvent);
  }

  public void setDelegate(reactor.core.scheduler.Scheduler delegate) {
    this.delegate = delegate;
  }
}
