/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.function.BiConsumer;

public class ExportableInitialSpanInfo implements InitialSpanInfo {

  private final InitialSpanInfo initialSpanInfo;
  private String exportableName;

  public ExportableInitialSpanInfo(InitialSpanInfo initialSpanInfo, String exportableName) {
    this.initialSpanInfo = initialSpanInfo;
    this.exportableName = exportableName;
  }

  @Override
  public String getName() {
    return exportableName;
  }

  public boolean isPolicySpan() {
    return initialSpanInfo.isPolicySpan();
  }

  public boolean isRootSpan() {
    return initialSpanInfo.isRootSpan();
  }

  public InitialExportInfo getInitialExportInfo() {
    return initialSpanInfo.getInitialExportInfo();
  }

  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    initialSpanInfo.forEachAttribute(biConsumer);
  }

  public int getInitialAttributesCount() {
    return initialSpanInfo.getInitialAttributesCount();
  }

  public void updateName(String name) {
    this.exportableName = name;
  }
}
