/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
