/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class GlobalErrorHandler extends ErrorHandler {

  private Consumer<Exception> consumer;

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  @Override
  public Consumer<Exception> router(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor,
                                    Consumer<CoreEvent> continueCallback, Consumer<Throwable> propagateCallback) {
    if (consumer == null) {
      consumer = super.router(publisherPostProcessor, continueCallback, propagateCallback);
    }

    return consumer;
  }

  @Override
  public void initialise() throws InitialisationException {
    setFromGlobalErrorHandler();
    super.initialise();
  }

  private void setFromGlobalErrorHandler() {
    List<MessagingExceptionHandlerAcceptor> listeners =
        this.getExceptionListeners().stream().map(exceptionListener -> (exceptionListener instanceof TemplateOnErrorHandler)
            ? ((TemplateOnErrorHandler) exceptionListener).setFromGlobalErrorHandler(true)
            : exceptionListener).collect(toList());
    this.setExceptionListeners(listeners);
  }

  public ErrorHandler createLocalErrorHandler(Location flowLocation) {
    ErrorHandler local = new ErrorHandler();
    local.setName(this.name);
    local.setExceptionListeners(this.getExceptionListeners());
    local.setExceptionListenersLocation(flowLocation);
    return local;
  }
}
