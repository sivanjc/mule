/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

import static java.lang.System.setProperty;

import static org.apache.logging.log4j.LogManager.setFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.logging.log4j.core.impl.Log4jContextFactory;

public class MuleLog4jContextFactory extends Log4jContextFactory {

  /**
   * Creates a new instance and sets it as the factory of the {@link org.apache.logging.log4j.LogManager}.
   *
   * @return the created {@link org.mule.runtime.module.boot.api.MuleLog4jContextFactory}.
   */
  /**
   * Creates a new instance and sets it as the factory of the {@link org.apache.logging.log4j.LogManager}.
   *
   * @return the created {@link MuleLog4jContextFactory}.
   */
  public static MuleLog4jContextFactory createAndInstall() {
    // We need to force the creation of a logger before we can change the manager factory.
    // This is because if not, any logger that will be acquired by MuleLog4jContextFactory code
    // will fail since it will try to use a null factory.
    getLogger("triggerDefaultFactoryCreation");
    // We need to set this property so log4j uses the same context factory everywhere
    setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
    MuleLog4jContextFactory log4jContextFactory = new MuleLog4jContextFactory();
    setFactory(log4jContextFactory);
    return log4jContextFactory;
  }
}
