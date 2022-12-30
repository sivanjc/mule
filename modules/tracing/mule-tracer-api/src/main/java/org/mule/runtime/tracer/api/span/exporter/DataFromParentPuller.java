/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span.exporter;

import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

public interface DataFromParentPuller<T extends SpanExporter> {

  void pullData(T parentTelemetrySpanExporter, T openTelemetrySpanExporter);

}
