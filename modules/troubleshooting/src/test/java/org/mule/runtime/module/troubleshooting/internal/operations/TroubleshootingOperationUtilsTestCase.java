/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationUtils;

import java.util.HashMap;
import java.util.Map;

public class TroubleshootingOperationUtilsTestCase {

  @Test
  public void zzz() {
    TroubleshootingOperation operation = TroubleshootingOperationUtils.createFrom(TestAnnotatedCallback.class);
    assertThat(operation.getDefinition().getName(), is("zaraza"));
  }

  @Test
  public void noExceptionThrownWhenCallingExecute() {
    TroubleshootingOperation operation = TroubleshootingOperationUtils.createFrom(TestAnnotatedCallback.class);
    Map<String, String> requiredArguments = new HashMap<>();
    requiredArguments.put("privateArgument", "A");
    requiredArguments.put("protectedArgument", "B");
    requiredArguments.put("publicArgument", "C");
    String result = (String) operation.getCallback().execute(requiredArguments);
    assertThat(result, is("ABCnull"));
  }
}
