/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.crac;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.crac.Core.checkpointRestore;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.RequestHandler;
import org.mule.runtime.http.api.server.RequestHandlerManager;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.crac.Core;

public class CracHandler implements Startable, Stoppable {

  @Inject
  private HttpService httpService;

  @Inject
  private SchedulerService schedulerService;

  private HttpServer httpServer;
  private RequestHandlerManager requestHandler;


  @Override
  public void start() throws MuleException {
    httpServer = httpService.getServerFactory().create(new HttpServerConfiguration.Builder()
        .setHost("localhost")
        .setPort(8080)
        .build());
    try {
      httpServer.start();
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }

    requestHandler = httpServer.addRequestHandler("/sleep", new RequestHandler() {

      @Override
      public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
        schedulerService.ioScheduler().schedule((CheckedRunnable) () -> checkpointRestore(), 0, MILLISECONDS);
      }
    });
  }

  @Override
  public void stop() throws MuleException {
    if (requestHandler != null) {
      requestHandler.stop();
      requestHandler.dispose();
    }

    if (httpServer != null) {
      httpServer.stop();
    }
  }
}
