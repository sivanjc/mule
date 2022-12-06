/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.extension.internal.ast;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.ast.api.ComponentGenerationInformation.EMPTY_GENERATION_INFO;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.ParameterResolutionException;
import org.mule.runtime.ast.internal.builder.PropertiesResolver;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AST component that represent a parameter of a configuration intended to be used when there is not a configuration on the
 * application but there are modules from the XML Sdk that have XML Sdk properties with default values.
 */
public class XmlSdkImplicitConfigParameter implements ComponentParameterAst {

  private static final String DEFAULT_EXPRESSION_PREFIX = "#[";
  private static final String DEFAULT_EXPRESSION_SUFFIX = "]";

  private static final Class<? extends ModelProperty> allowsExpressionWithoutMarkersModelPropertyClass;
  private static final Map<String, Function<String, Number>> fixedNumberMappings = new HashMap<>();

  static {
    Class<? extends ModelProperty> foundClass = null;
    try {
      foundClass = (Class<? extends ModelProperty>) Class
          .forName("org.mule.runtime.module.extension.api.loader.java.property.AllowsExpressionWithoutMarkersModelProperty");
    } catch (ClassNotFoundException | SecurityException e) {
      // No custom location processing
    }
    allowsExpressionWithoutMarkersModelPropertyClass = foundClass;

    fixedNumberMappings.put(Integer.class.getName(), Integer::valueOf);
    fixedNumberMappings.put(int.class.getName(), Integer::valueOf);

    fixedNumberMappings.put(Float.class.getName(), Float::valueOf);
    fixedNumberMappings.put(float.class.getName(), Float::valueOf);

    fixedNumberMappings.put(Long.class.getName(), Long::valueOf);
    fixedNumberMappings.put(long.class.getName(), Long::valueOf);

    fixedNumberMappings.put(Byte.class.getName(), Byte::valueOf);
    fixedNumberMappings.put(byte.class.getName(), Byte::valueOf);

    fixedNumberMappings.put(Short.class.getName(), Short::valueOf);
    fixedNumberMappings.put(short.class.getName(), Short::valueOf);

    fixedNumberMappings.put(Double.class.getName(), Double::valueOf);
    fixedNumberMappings.put(double.class.getName(), Double::valueOf);

    fixedNumberMappings.put(BigDecimal.class.getName(), BigDecimal::new);
    fixedNumberMappings.put(BigInteger.class.getName(), BigInteger::new);
  }

  private final ParameterModel parameterModel;

  private final ParameterGroupModel parameterGroupModel;

  private volatile LazyValue<String> resolved;
  private volatile LazyValue<Either<String, Object>> value;

  public XmlSdkImplicitConfigParameter(ParameterGroupModel parameterGroupModel, ParameterModel parameterModel, Object value,
                                       ConfigurationPropertiesResolver configurationPropertiesResolver) {
    this.parameterGroupModel = parameterGroupModel;
    this.parameterModel = parameterModel;
    resolveValue(value, configurationPropertiesResolver);
  }

  @Override
  public ParameterModel getModel() {
    return parameterModel;
  }

  @Override
  public ParameterGroupModel getGroupModel() {
    return parameterGroupModel;
  }

  private void resolveValue(Object rawValue, ConfigurationPropertiesResolver configurationPropertiesResolver) {
    if (!(rawValue instanceof String)) {
      value = new LazyValue<>(() -> right(rawValue));
      return;
    }
    String stringRawValue = (String) rawValue;
    if (stringRawValue != null && hasPropertyPlaceholder(stringRawValue)) {
      this.resolved = new LazyValue<>(() -> configurationPropertiesResolver.apply(stringRawValue));
    } else {
      this.resolved = new LazyValue<>(stringRawValue);
    }

    this.value = new LazyValue<>(() -> {
      String resolvedRawValue = getResolvedRawValue();

      if (isNull(resolvedRawValue)) {
        final Object defaultValue = getModel().getDefaultValue();

        if (defaultValue != null) {
          if (defaultValue instanceof String) {
            resolvedRawValue = (String) defaultValue;
          } else if (getModel().getType().getAnnotation(EnumAnnotation.class).isPresent()) {
            resolvedRawValue = ((Enum<?>) defaultValue).name();
          } else {
            return right(defaultValue);
          }
        }
      }

      return resolveParamValue(resolvedRawValue);
    });
  }

  private Either<String, Object> resolveParamValue(String resolvedRawValue) {
    AtomicReference<String> expression = new AtomicReference<>();
    AtomicReference<Object> fixedValue = new AtomicReference<>();

    final MetadataTypeVisitor visitor = new MetadataTypeVisitor() {

      @Override
      public void visitObjectField(ObjectFieldType objectFieldType) {
        objectFieldType.getValue().accept(this);
      }

      @Override
      public void visitBoolean(BooleanType booleanType) {
        doVisitPrimitive(booleanType, () -> {
          if (!isEmpty(resolvedRawValue)) {
            fixedValue.set(Boolean.valueOf(resolvedRawValue));
          }
        });
      }

      @Override
      public void visitNumber(NumberType numberType) {
        doVisitPrimitive(numberType, () -> {
          if (!isEmpty(resolvedRawValue)) {
            visitFixedNumber(resolvedRawValue, fixedValue, numberType);
          }
        });
      }

      @Override
      public void visitString(StringType stringType) {
        doVisitPrimitive(stringType, () -> {
          // Empty string is valid, do not return either.empty for this!
          fixedValue.set(resolvedRawValue);
        });
      }

      private void doVisitPrimitive(MetadataType metadataType, Runnable onFixedValue) {
        if (isExpression(resolvedRawValue) || hasPropertyPlaceholder(resolvedRawValue)) {
          defaultVisit(metadataType);
        } else {
          onFixedValue.run();
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (!getModel().getAllowedStereotypes().isEmpty() && resolvedRawValue != null) {
          // For references, just return the name of the referenced object if it is a fixed value, but the param may be an
          // expression that builds an object of the expected stereotype
          defaultVisitFixedValue(resolvedRawValue, expression, fixedValue);
        } else if (NOT_SUPPORTED.equals(getModel().getExpressionSupport())
            || getModel().getType().getAnnotation(LiteralTypeAnnotation.class).isPresent()) {
          fixedValue.set(resolvedRawValue);
        } else if (!NOT_SUPPORTED.equals(getModel().getExpressionSupport())) {
          defaultVisitFixedValue(resolvedRawValue, expression, fixedValue);
        } else {
          final Optional<String> extractExpression = extractExpression(resolvedRawValue);
          if (extractExpression.isPresent()) {
            expression.set(extractExpression.get());
          } else {
            fixedValue.set(resolvedRawValue);
          }
        }
      }
    };

    try {
      getModel().getType().accept(visitor);
    } catch (Exception e) {
      throw new ParameterResolutionException(createStaticMessage("Exception resolving param"),
                                             e);
    }

    if (expression.get() != null) {
      return left(expression.get());
    } else if (fixedValue.get() != null) {
      return right(fixedValue.get());
    } else {
      return Either.empty();
    }
  }

  private void visitFixedNumber(String rawValue, AtomicReference<Object> value, NumberType numberType) {
    value.set(numberType.getAnnotation(ClassInformationAnnotation.class)
        .map(classInfo -> fixedNumberMappings.getOrDefault(classInfo.getClassname(), s -> null).apply(rawValue))
        .orElseGet(() -> {
          try {
            Long longValue = Long.valueOf(rawValue);
            if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
              return longValue.intValue();
            }
            return longValue;
          } catch (NumberFormatException e) {
            return Double.valueOf(rawValue);
          }
        }));
  }

  private void defaultVisitFixedValue(String rawValue, AtomicReference<String> expression, AtomicReference<Object> fixedValue) {
    Optional<String> expressionOpt = extractExpression(rawValue);
    if (expressionOpt.isPresent()) {
      // For complex types that may be the result of an expression, just return the expression
      expression.set(expressionOpt.get());
    } else {
      fixedValue.set(rawValue);
    }
  }

  private boolean isExpression(Object value) {
    if (value instanceof String) {
      String trim = ((String) value).trim();

      if (trim.startsWith(DEFAULT_EXPRESSION_PREFIX) && trim.endsWith(DEFAULT_EXPRESSION_SUFFIX)) {
        return true;
      }

      return allowsExpressionWithoutMarkersModelPropertyClass != null
          && getModel().getModelProperty(allowsExpressionWithoutMarkersModelPropertyClass).isPresent();
    } else {
      return false;
    }
  }

  private boolean hasPropertyPlaceholder(String v) {
    return v != null && v.contains("${");
  }

  /**
   * Parse the given value and remove expression markers if it is considered as an expression.
   *
   * @param value Value to parse
   * @return a String containing the expression without markers or null if the value is not an expression.
   */
  public Optional<String> extractExpression(Object value) {
    Optional<String> result = empty();
    if (isExpression(value)) {
      String expression = (String) value;
      if (isNotEmpty(expression)) {
        String trimmedText = expression.trim();

        if (trimmedText.startsWith(DEFAULT_EXPRESSION_PREFIX) && trimmedText.endsWith(DEFAULT_EXPRESSION_SUFFIX)) {
          result =
              of(trimmedText.substring(DEFAULT_EXPRESSION_PREFIX.length(),
                                       trimmedText.length() - DEFAULT_EXPRESSION_SUFFIX.length()));
        } else {
          result = of(trimmedText);
        }
      }
    }

    return result;
  }

  @Override
  public <T> Either<String, T> getValue() {
    return (Either<String, T>) value.get();
  }

  @Override
  public <T> Either<String, Either<ParameterResolutionException, T>> getValueOrResolutionError() {
    return null;
  }

  @Override
  public String getRawValue() {
    return value != null ? value.toString() : null;
  }

  @Override
  public String getResolvedRawValue() {
    return resolved == null ? null : resolved.get();
  }

  @Override
  public Optional<ComponentMetadataAst> getMetadata() {
    return empty();
  }

  @Override
  public ComponentGenerationInformation getGenerationInformation() {
    return EMPTY_GENERATION_INFO;
  }

  @Override
  public boolean isDefaultValue() {
    return true;
  }
}
