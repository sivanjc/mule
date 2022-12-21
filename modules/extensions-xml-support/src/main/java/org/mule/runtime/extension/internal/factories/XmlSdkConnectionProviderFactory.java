/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.NO_CONSTRUCTORS;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;

import java.util.Map;
import java.util.Optional;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;


public class XmlSdkConnectionProviderFactory implements ConnectionProviderFactory {

  private final ConnectionProviderModel innerConnectionProviderModel;
  private final ConnectionProviderFactory delegateConnectionProviderFactory;
  private final Map<ParameterModel, String> connectionParamsDefaultValues;;

  public XmlSdkConnectionProviderFactory(ConnectionProviderModel innerConnectionProviderModel,
                                         ConnectionProviderFactory delegateConnectionProviderFactory,
                                         Map<ParameterModel, String> connectionParamsDefaultValues) {
    this.innerConnectionProviderModel = innerConnectionProviderModel;
    this.delegateConnectionProviderFactory = delegateConnectionProviderFactory;
    this.connectionParamsDefaultValues = connectionParamsDefaultValues;
  }

  @Override
  public ConnectionProvider newInstance() {
    // This will, instead, need to generate a dynamic class with the setters for the connection parameters of the extension, and
    // the impl oif that generated class will have to forward them to the proper parameter in the delegate.

    // extracted();

    try {
      ParametersResolver parametersResolver = ParametersResolver.fromValues(connectionParamsDefaultValues
          .entrySet()
          .stream()
          .collect(toMap(e -> e.getKey().getName(), e -> e.getValue())), null, null, null,
                                                                            innerConnectionProviderModel.getName());

      ResolverSet typeUnsafeResolverSet = parametersResolver.getParametersAsResolverSet(null,
                                                                                        innerConnectionProviderModel,
                                                                                        innerConnectionProviderModel
                                                                                            .getParameterGroupModels());

      Map<String, ParameterModel> paramModels =
          innerConnectionProviderModel.getAllParameterModels().stream().collect(toMap(p -> p.getName(), identity()));

      ResolverSet typeSafeResolverSet = new ResolverSet(null);
      typeUnsafeResolverSet.getResolvers().forEach((paramName, resolver) -> {
        ParameterModel model = paramModels.get(paramName);
        if (model != null) {
          Optional<Class<Object>> clazz = getType(model.getType());
          if (clazz.isPresent()) {
            resolver = new TypeSafeValueResolverWrapper(resolver, clazz.get());
          }
        }

        typeSafeResolverSet.add(paramName, resolver);
      });

      typeSafeResolverSet.initialise();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }

    return delegateConnectionProviderFactory.newInstance();
  }

  protected void extracted() {
    ClassLoader artifactClassLoader = Thread.currentThread().getContextClassLoader();

    DynamicType.Builder<Object> connectionProviderWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, NO_CONSTRUCTORS)
        .implement(MethodExecutor.class)
        .name("A");

    Initial<Object> constructorDefinition = connectionProviderWrapperClassBuilder
        .defineConstructor(PUBLIC);

    connectionProviderWrapperClassBuilder
        .defineMethod("setA", Void.class, PUBLIC)
        .withParameter(String.class, "a");

    final Unloaded<Object> byteBuddyMadeWrapper = constructorDefinition.intercept(new Implementation() {

      @Override
      public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
      }

      @Override
      public ByteCodeAppender appender(Target implementationTarget) {
        return null;
      }
    })
        .make();

    // try {
    Class<? extends Object> loadedClass = byteBuddyMadeWrapper.load(artifactClassLoader, Default.INJECTION).getLoaded();
    // } catch (Exception e) {
    // throw new MuleRuntimeException(createStaticMessage("Could not generate "),
    // e);
    // }
  }

  @Override
  public Class getObjectType() {
    return delegateConnectionProviderFactory.getObjectType();
  }

}
