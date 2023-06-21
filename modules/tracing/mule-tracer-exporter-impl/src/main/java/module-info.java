/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Implementation for the tracer exporter.
 * 
 * @since 4.5
 */
module org.mule.runtime.tracer.exporter.impl {

  requires org.mule.runtime.api;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;
  requires org.mule.runtime.tracer.exporter.config.impl;

  // Just to manifest the error
  requires io.grpc;

  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.exporter.internal;
  requires io.opentelemetry.exporter.otlp;
  requires io.opentelemetry.sdk.common;
  requires io.opentelemetry.sdk.metrics;
  requires io.opentelemetry.sdk.trace;

  requires javax.inject;

  requires com.google.gson;
}