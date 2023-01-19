/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link ResetOnQueryCounter} that holds the counter in an {@link AtomicLong}.
 * 
 * @since 4.5
 */
public class DefaultResetOnQueryCounter implements ResetOnQueryCounter {

  private final AtomicLong counter;
  private final AtomicLong accumulator = new AtomicLong(0);

  public DefaultResetOnQueryCounter(AtomicLong counter) {
    this.counter = counter;
  }

  //  private final AtomicLong counter = new AtomicLong(0);

  @Override
  public long getAndReset() {
    long realValue = counter.get();
    return realValue - accumulator.getAndSet(realValue);
  }

  @Override
  public long get() {
    return counter.get() - accumulator.get();
  }

  void clear() {
    accumulator.set(0);
  }

//  public void increment() {
//    counter.incrementAndGet();
//  }
//
//  public void add(long value) {
//    counter.addAndGet(value);
//  }
}
