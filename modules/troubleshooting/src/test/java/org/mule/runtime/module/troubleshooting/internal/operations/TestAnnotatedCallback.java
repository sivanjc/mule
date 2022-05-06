/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import org.mule.runtime.module.troubleshooting.api.AnnotatedTroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.Argument;
import org.mule.runtime.module.troubleshooting.api.Operation;

import java.io.Serializable;

@Operation(name = "zaraza", description = "some desc")
public class TestAnnotatedCallback implements AnnotatedTroubleshootingOperationCallback {

  @Argument(required = false)
  String pkgArgument;

  @Argument(description = "private")
  private String privateArgument;

  @Argument
  public String publicArgument;

  @Argument
  protected String protectedArgument;

  @Override
  public Serializable execute() {
    return privateArgument + protectedArgument + publicArgument + pkgArgument;
  }
}
