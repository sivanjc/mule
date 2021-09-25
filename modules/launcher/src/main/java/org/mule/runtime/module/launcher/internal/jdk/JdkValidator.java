/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.internal.jdk;

import static org.apache.commons.lang3.SystemUtils.JAVA_VERSION;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.invalidJdk;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.getSupportedJdks;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.util.JdkVersionUtils;

/**
 * Validates that the JDK Mule is running is a supported one.
 * 
 * @since 4.5
 */
public class JdkValidator implements Initialisable {

  @Override
  public void initialise() throws InitialisationException {
    try {
      JdkVersionUtils.validateJdk();
    } catch (RuntimeException e) {
      throw new InitialisationException(invalidJdk(JAVA_VERSION, getSupportedJdks()), this);
    }
  }

}
