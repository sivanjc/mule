/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

public interface InternalSpanPrototypeRepository {

  void addPrototype(InitialSpanInfo initialSpanInfo, SpanExporter spanExporter);

  InternalSpan getPrototypedInternalSpan(InitialSpanInfo initialSpanInfo);

}
