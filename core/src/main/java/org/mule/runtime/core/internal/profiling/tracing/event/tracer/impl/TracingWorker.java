/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Queue;

public class TracingWorker implements Runnable {

  private final Queue<TracingCommand> commandQueue;
  private final BlockingQueue<Boolean> signal = new ArrayBlockingQueue<>(1);
  private static final long POLL_TIMEOUT = 1000L;
  private boolean continueWork = true;

  public TracingWorker() {
    this.commandQueue = new ArrayBlockingQueue<>(1000);
  }

  @Override
  public void run() {
    while (continueWork) {
      while (!commandQueue.isEmpty()) {
        commandQueue.poll().execute();
      }

      if (commandQueue.isEmpty()) {
        try {
          signal.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  public void addTracingCommand(TracingCommand tracingCommand) {
    signal.offer(true);
    commandQueue.add(tracingCommand);
  }

  public void shutDown() {
    continueWork = false;
  }
}
