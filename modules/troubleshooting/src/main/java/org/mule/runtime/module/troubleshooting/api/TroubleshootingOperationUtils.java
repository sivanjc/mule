/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.reflections.ReflectionUtils.getAllFields;

import org.mule.runtime.module.troubleshooting.internal.DefaultArgumentDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TroubleshootingOperationUtils {

  public static <T extends AnnotatedTroubleshootingOperationCallback> TroubleshootingOperation createFrom(Class<T> annotatedClass) {
    return createFrom(annotatedClass, () -> {
      try {
        return annotatedClass.getConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        e.printStackTrace();
      }
      return null;
    });
  }

  public static <T extends AnnotatedTroubleshootingOperationCallback> TroubleshootingOperation createFrom(Class<T> annotatedClass,
                                                                                                          Supplier<T> callbackSupplier) {
    if (!annotatedClass.isAnnotationPresent(Operation.class)) {
      throw new IllegalStateException("Class is not annotated");
    }

    Operation operationAnnotation = annotatedClass.getAnnotation(Operation.class);
    String operationName = operationAnnotation.name();
    String operationDescription = operationAnnotation.description();

    List<ArgumentDefinition> argumentDefinitionList =
        getAllFields(annotatedClass, field -> field.isAnnotationPresent(Argument.class)).stream().map(field -> {
          Argument argumentAnnotation = field.getAnnotation(Argument.class);
          return new DefaultArgumentDefinition(field.getName(), argumentAnnotation.description(), argumentAnnotation.required());
        }).collect(toList());

    Map<String, Field> fieldsByName =
        getAllFields(annotatedClass, field -> field.isAnnotationPresent(Argument.class)).stream()
            .collect(toMap(Field::getName, identity()));

    TroubleshootingOperationDefinition definition =
        new DefaultTroubleshootingOperationDefinition(operationName, operationDescription,
                                                      argumentDefinitionList.toArray(new ArgumentDefinition[0]));

    return new TroubleshootingOperation() {

      @Override
      public TroubleshootingOperationDefinition getDefinition() {
        return definition;
      }

      @Override
      public TroubleshootingOperationCallback getCallback() {
        return arguments -> {
          try {
            AnnotatedTroubleshootingOperationCallback callback = callbackSupplier.get();

            for (ArgumentDefinition argumentDefinition : argumentDefinitionList) {
              String valueToSet = arguments.get(argumentDefinition.getName());
              if (argumentDefinition.isRequired() && valueToSet == null) {
                throw new IllegalArgumentException(format("Missing argument '%s'", argumentDefinition.getName()));
              }

              Field fieldToSet = fieldsByName.get(argumentDefinition.getName());
              fieldToSet.setAccessible(true);
              fieldToSet.set(callback, valueToSet);
            }

            return callback.execute();
          } catch (Exception e) {
            // By the time being we return the message.
            return e.getMessage();
          }
        };
      }
    };
  }
}
