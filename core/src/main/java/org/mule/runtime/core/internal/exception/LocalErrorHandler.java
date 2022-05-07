/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;

public class LocalErrorHandler extends ErrorHandler {

  private final GlobalErrorHandler globalErrorHandler;

  LocalErrorHandler(GlobalErrorHandler globalErrorHandler) {
    this.globalErrorHandler = globalErrorHandler;
  }

  @Override
  public void stop() throws MuleException {
    if (logger.isDebugEnabled()) {
      logger.debug("Stopping local error handler: {}. Nothing will be done as this depends on a global error handler.", this);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    globalErrorHandler.initialiseErrorListenerProcessorIfNeeded();
  }

  public void stopParent() throws MuleException {
    super.stop();
  }
}
