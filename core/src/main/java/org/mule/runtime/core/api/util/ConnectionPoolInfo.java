/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

public class ConnectionPoolInfo {

  private int numActive;
  private int numIdle;
  private int maxActive;
  private int maxIdle;
  private long maxWait;

  public ConnectionPoolInfo(int numActive, int numIdle, int maxActive, int maxIdle, long maxWait) {
    this.numActive = numActive;
    this.numIdle = numIdle;
    this.maxActive = maxActive;
    this.maxIdle = maxIdle;
    this.maxWait = maxWait;
  }
}
