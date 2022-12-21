/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.NO_CONSTRUCTORS;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutor;

import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;


public class XmlSdkConnectionProviderFactory implements ConnectionProviderFactory {

  private final String name;
  private final ConnectionProviderFactory delegateConnectionProviderFactory;
  private final Map<ParameterModel, String> connectionParamsDefaultValues;;

  public XmlSdkConnectionProviderFactory(String name,
                                         ConnectionProviderFactory delegateConnectionProviderFactory,
                                         Map<ParameterModel, String> connectionParamsDefaultValues) {
    this.name = name;
    this.delegateConnectionProviderFactory = delegateConnectionProviderFactory;
    this.connectionParamsDefaultValues = connectionParamsDefaultValues;
  }

  @Override
  public ConnectionProvider newInstance() {
    // This will, instead, need to generate a dynamic class with the setters for the connection parameters of the extension, and
    // the impl oif that generated class will have to forward them to the proper parameter in the delegate.

    ClassLoader artifactClassLoader = Thread.currentThread().getContextClassLoader();

    DynamicType.Builder<Object> connectionProviderWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, NO_CONSTRUCTORS)
        .implement(MethodExecutor.class)
        .name(name);

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

    return delegateConnectionProviderFactory.newInstance();
  }

  @Override
  public Class getObjectType() {
    return delegateConnectionProviderFactory.getObjectType();
  }

}
