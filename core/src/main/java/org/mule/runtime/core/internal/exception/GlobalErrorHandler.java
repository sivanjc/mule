/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.util.MuleSystemProperties.REVERT_SIGLETON_ERROR_HANDLER_PROPERTY;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.event.CoreEvent;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalErrorHandler extends ErrorHandler {

  private static final boolean IS_PROTOTYPE = getBoolean(REVERT_SIGLETON_ERROR_HANDLER_PROPERTY);
  private LazyValue<List<MessagingExceptionHandlerAcceptor>> wrappedExceptionListeners =
      new LazyValue<List<MessagingExceptionHandlerAcceptor>>(this::getWrappedExceptionListeners);


  // We need to keep a reference to one of the local error handlers to be able to stop its inner processors.
  // This is a temporary solution and won't be necessary after W-10674245.
  // TODO: W-10674245 remove this
  private ErrorHandler local;
  private AtomicBoolean initialised = new AtomicBoolean(false);
  private AtomicBoolean started = new AtomicBoolean(false);

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  @Override
  public void stop() throws MuleException {
    if (!IS_PROTOTYPE) {
      ((LocalErrorHandler) local).stopParent();
    }
  }

  public ErrorHandler createLocalErrorHandler(Location flowLocation) {
    ErrorHandler local;
    if (IS_PROTOTYPE) {
      local = new ErrorHandler();
    } else {
      local = new LocalErrorHandler();
    }
    local.setName(this.name);


    local.setExceptionListeners(wrappedExceptionListeners.get());
    local.setExceptionListenersLocation(flowLocation);
    if (this.local == null) {
      this.local = local;
    }
    return local;
  }

  private List<MessagingExceptionHandlerAcceptor> getWrappedExceptionListeners() {
    List<MessagingExceptionHandlerAcceptor> exceptionListeners = this.getExceptionListeners();
    exceptionListeners.forEach(exceptionListener -> {
      if (exceptionListener instanceof TemplateOnErrorHandler) {
        TemplateOnErrorHandler templateOnErrorHandler = ((TemplateOnErrorHandler) exceptionListener);
        templateOnErrorHandler.setMessageProcessors(wrapProcessors(templateOnErrorHandler.getMessageProcessors()));
      }
    });

    return exceptionListeners;
  }

  private List<Processor> wrapProcessors(List<Processor> messageProcessors) {
    List<Processor> wrappedProccesors = new ArrayList<>();

    messageProcessors.forEach(processor -> {
      wrappedProccesors.add(new WrappedProcessor(this, processor));
    });
    return wrappedProccesors;
  }

  private class WrappedProcessor implements Processor, Lifecycle {

    private final GlobalErrorHandler globalErrorHandler;
    private final Processor delegate;

    public WrappedProcessor(GlobalErrorHandler globalErrorHandler, Processor processor) {
      this.globalErrorHandler = globalErrorHandler;
      this.delegate = processor;
    }

    @Override
    public void dispose() {
      if (delegate instanceof Disposable) {
        ((Disposable) delegate).dispose();
      }
    }

    @Override
    public void initialise() throws InitialisationException {
      if (!globalErrorHandler.getInitialised().getAndSet(true) && delegate instanceof Initialisable) {
        ((Initialisable) delegate).initialise();
      }
    }

    @Override
    public void start() throws MuleException {
      if (!globalErrorHandler.getStarted().getAndSet(true) && delegate instanceof Startable) {
        ((Startable) delegate).start();
      }
    }

    @Override
    public void stop() throws MuleException {
      if (delegate instanceof Stoppable) {
        ((Stoppable) delegate).stop();
      }
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return delegate.process(event);
    }
  }

  private AtomicBoolean getInitialised() {
    return initialised;
  }

  private AtomicBoolean getStarted() {
    return started;
  }
}
