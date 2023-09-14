/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api.discoverer;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

public final class ContainerDiscovererHelper {

  private ContainerDiscovererHelper() {
    // nothing to do
  }

  public static void ooo(Class clazz) {
    ContainerDiscovererHelper.class.getModule()
        .addExports("org.mule.runtime.container.internal",
                    StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass().getModule());

  }
}
