/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import java.lang.reflect.Field;

abstract class FieldReferenceModelProperty implements SarazaInterface {

  private final String className;
  private final String fieldName;

  private transient Field field;

  protected FieldReferenceModelProperty(Field declaringField) {
    field = declaringField;
    className = declaringField.getDeclaringClass().getName();
    fieldName = declaringField.getName();
  }

  protected FieldReferenceModelProperty(String className, String fieldName) {
    this.className = className;
    this.fieldName = fieldName;
  }

  protected Field getField() {
    return field;
  }


}
