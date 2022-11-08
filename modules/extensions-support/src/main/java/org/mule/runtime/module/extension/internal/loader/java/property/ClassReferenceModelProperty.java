/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

abstract class ClassReferenceModelProperty implements SarazaInterface {

  private final String className;
  private transient Type type;

  /**
   * Creates a new instance
   *
   * @param clazz a connection type
   */
  protected ClassReferenceModelProperty(Class<?> clazz) {
    this(new TypeWrapper(clazz));
  }

  public ClassReferenceModelProperty(Type type) {
    className = type.getTypeName();
    this.type = type;
  }

  protected Type getType() {
    return type;
  }
}
