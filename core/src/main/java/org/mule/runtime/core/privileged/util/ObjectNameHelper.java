/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.lang.String.format;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates consistent objects names for Mule components
 */
// @ThreadSafe
public class ObjectNameHelper {

  private final static AtomicInteger autoGeneratedIndex = new AtomicInteger(0);
  private MuleContext muleContext;


  public ObjectNameHelper(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * @param prefix prefix to use for the name
   * @return a new name that's unique
   */
  public String getUniqueName(String prefix) {
    String name;
    do {
      name = format(prefix + "-%s", autoGeneratedIndex.getAndIncrement());
    } while (((MuleContextWithRegistry) muleContext).getRegistry().get(name) != null);
    return name;
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }
}
