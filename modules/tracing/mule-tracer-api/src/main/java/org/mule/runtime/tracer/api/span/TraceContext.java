/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public interface TraceContext {

  TraceContext EMPTY = new EmptyTraceContext();

  String getTraceId();

  String getSpanId();

  default void setTraceId(String traceId) {}

  default void setSpanId(String spanId) {}

  default Map<String, String> getAttributesSetBySource() {
    return Collections.emptyMap();
  }

  default Optional<String> getNameSetBySource() {
    return empty();
  }

  default void setAttributeSetBySource(String key, String value) {}

  default void setLastExportableSpan(InternalSpan lastExportableSpan) {}

  default void setNameSetBySource(String name) {}

  InternalSpan getLastUpdatableByExtensionSpan();

  default void setLastUpdatableByExtensionSpan(InternalSpan lastUpdatableByExtensionSpan) {
    // Nothing to do by default.
  }

  Map<String, String> getRemoteTraceSerialization();

  default void clearAttributesSetBySource() {}

  InternalSpan getLastExporableSpan();

  default Optional<InitialExportInfo> getInitialExportInfo() {
    return empty();
  }

  default void setInitialExportInfo(InitialExportInfo initialSpanInfo) {}

  default Map<String, String> getTraceState() {
    return null;
  }

  default void setTraceState(Map<String, String> traceState) {
    // Nothing to do by default
  }


  class EmptyTraceContext implements TraceContext {

    @Override
    public String getTraceId() {
      return null;
    }

    @Override
    public String getSpanId() {
      return null;
    }

    @Override
    public InternalSpan getLastUpdatableByExtensionSpan() {
      return null;
    }

    @Override
    public Map<String, String> getRemoteTraceSerialization() {
      return emptyMap();
    }

    @Override
    public InternalSpan getLastExporableSpan() {
      return null;
    }
  }
}
