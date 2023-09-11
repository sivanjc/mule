/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Operations({BleOperations.class})
@Configuration(name = "bleconf")
public class BleConfig extends ConfigWithNumber {

  public int getNumber() {
    return number;
  }

  @Parameter
  @Optional(defaultValue = "5")
  int number;

  @Parameter
  @Optional
  @NullSafe(defaultImplementingType = NullSafeImplementation.class)
  NullSafeInterface nullSafeParameter;
}
