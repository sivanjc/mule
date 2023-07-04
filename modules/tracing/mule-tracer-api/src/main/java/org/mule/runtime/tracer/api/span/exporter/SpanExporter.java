/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span.exporter;

import static org.mule.runtime.api.profiling.tracing.SpanIdentifier.INVALID_SPAN_IDENTIFIER;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;

import java.util.Map;

/**
 * An exporter for {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface SpanExporter {

  SpanExporter NOOP_EXPORTER = new SpanExporter() {

    @Override
    public void export() {
      // Nothing to do.
    }

    @Override
    public Map<String, String> exportedSpanAsMap() {
      return emptyMap();
    }

  };

  /**
   * Exports the {@link InternalSpan}.
   */
  void export();

  /**
   * @return the exported span as a map.
   */
  Map<String, String> exportedSpanAsMap();
}
