/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * {@link MethodElement} specification for Functions
 *
 * @since 4.1
 */
@NoImplement
public interface FunctionElement extends MethodElement<FunctionContainerElement> {

  /**
   * {@inheritDoc}
   */
  FunctionContainerElement getEnclosingType();

}
