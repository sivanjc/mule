/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.ServiceLoader.load;

import org.mule.runtime.module.boot.api.MuleContainerLifecycleWrapper;
import org.mule.runtime.module.boot.api.MuleContainerLifecycleWrapper.MuleContainerLifecycleWrapperProvider;

/**
 * Helps with the creation and provisioning of the {@link MuleContainerWrapper} implementation instance as a singleton.
 * <p>
 * Only thread-safe for threads started after the first provisioning.
 *
 * @since 4.5
 */
public class DefaultMuleContainerLifecycleWrapperProvider {

  private static MuleContainerLifecycleWrapper INSTANCE;

  /**
   * Creates the implementation instance based on the system property
   * {@link #MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY}.
   *
   * @return The {@link MuleContainerWrapper} implementation.
   */
  public static MuleContainerLifecycleWrapper getMuleContainerWrapper() {
    if (INSTANCE == null) {
      INSTANCE = createContainerWrapper();
    }

    return INSTANCE;
  }

  /**
   * Creates the implementation instance based on the system property
   * {@link #MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY}.
   *
   * @return The {@link MuleContainerWrapper} implementation.
   */
  private static MuleContainerLifecycleWrapper createContainerWrapper() {
    // el class loader podria ser el de aca total se que en embedded esto no se tiene que ejecutar y en standalone boot-commons va
    // a estar en el mismo Loader boot-api y por ende que esta clase
    MuleContainerLifecycleWrapperProvider wrapper =
        load(MuleContainerLifecycleWrapperProvider.class, MuleContainerLifecycleWrapper.class.getClassLoader()).iterator().next();
    return wrapper.provide();
  }
}
