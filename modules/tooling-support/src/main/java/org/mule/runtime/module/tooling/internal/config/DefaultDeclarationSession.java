/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.sdk.api.values.ValueResolvingException.CONNECTION_FAILURE;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticService;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDeclarationSession extends AbstractArtifactAgnosticService implements DeclarationSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDeclarationSession.class);
  private LazyValue<DeclarationSession> internalDeclarationSession;

  DefaultDeclarationSession(ApplicationSupplier applicationSupplier) {
    super(applicationSupplier);
    this.internalDeclarationSession = new LazyValue<>(() -> {
      try {
        return createInternalService(getStartedApplication());
      } catch (ApplicationStartingException e) {
        Exception causeException = e.getCauseException();
        propagateIfPossible(causeException, MuleRuntimeException.class);
        throw new MuleRuntimeException(causeException);
      }
    });
  }

  private DeclarationSession createInternalService(Application application) {
    long startTime = currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creating declaration session to delegate calls");
    }

    final InternalDeclarationSession internalDeclarationService =
        new InternalDeclarationSession(application.getDescriptor().getArtifactDeclaration());
    InternalDeclarationSession internalDeclarationSession = application.getRegistry()
        .lookupByType(MuleContext.class)
        .map(muleContext -> {
          try {
            return muleContext.getInjector().inject(internalDeclarationService);
          } catch (MuleException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not inject values into DeclarationSession"));
          }
        })
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find injector to create InternalDeclarationSession")));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creation of declaration session to delegate calls took [{}ms]", currentTimeMillis() - startTime);
    }

    return internalDeclarationSession;
  }

  private <T> T withInternalDeclarationSession(String functionName, Function<DeclarationSession, T> function) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Calling function: '{}'", functionName);
    }
    DeclarationSession declarationSession = getInternalDeclarationSession();

    long initialTime = currentTimeMillis();
    try {
      return function.apply(declarationSession);
    } finally {
      long totalTimeSpent = currentTimeMillis() - initialTime;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Function: '{}' completed in [{}ms]", functionName, totalTimeSpent);
      }
    }
  }

  private DeclarationSession getInternalDeclarationSession() {
    return this.internalDeclarationSession.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    try {
      return withInternalDeclarationSession("testConnection()", session -> session.testConnection(configName));
    } catch (DeploymentStartException e) {
      String message = format("Couldn't start configuration(s) while performing test connection on config: '%s'", configName);
      LOGGER.error(message, e);
      return getCausalChain(e).stream()
          .filter(exception -> exception.getClass().equals(ConnectionException.class)
              && ((ConnectionException) exception).getErrorType().isPresent())
          .map(exception -> failure(exception.getMessage(),
                                    ((ConnectionException) exception).getErrorType().get(),
                                    (Exception) exception))
          .findFirst()
          .orElse(failure(message, e));
    } catch (Throwable t) {
      LOGGER.error(format("Error while performing test connection on config: '%s'", configName), t);
      throw t;
    }
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String providerName) {
    try {
      return withInternalDeclarationSession("getValues()", session -> session.getValues(component, providerName));
    } catch (DeploymentStartException e) {
      String message =
          format("Couldn't start configuration(s) while resolving values on component: '%s:%s' for providerName: '%s'",
                 component.getDeclaringExtension(),
                 component.getName(), providerName);
      LOGGER.error(message, e);
      return resultFrom(ResolvingFailure.Builder.newFailure(e)
          .withMessage(message)
          .withReason(getRootCauseMessage(e))
          .build());
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving values on component: '%s:%s' for providerName: '%s'",
                          component.getDeclaringExtension(),
                          component.getName(), providerName),
                   t);
      throw t;
    }
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getMetadataKeys()", session -> session.getMetadataKeys(component));
    } catch (DeploymentStartException e) {
      String message = format("Couldn't start configuration(s) while resolving metadata keys on component: '%s:%s'",
                              component.getDeclaringExtension(),
                              component.getName());
      LOGGER.error(message, e);
      return MetadataResult.failure(MetadataFailure.Builder.newFailure(e)
          .withMessage(message)
          .withReason(getRootCauseMessage(e)).onKeys());
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving metadata keys on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("resolveComponentMetadata()", session -> session.resolveComponentMetadata(component));
    } catch (DeploymentStartException e) {
      String message = format("Couldn't start configuration(s) while resolving metadata on component: '%s:%s'",
                              component.getDeclaringExtension(),
                              component.getName());
      LOGGER.error(message, e);
      return MetadataResult.failure(MetadataFailure.Builder
          .newFailure(e)
          .withMessage(message)
          .withReason(getRootCauseMessage(e)).onComponent());
    } catch (Throwable t) {
      LOGGER.error(format("Error while resolving metadata on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public SampleDataResult getSampleData(ComponentElementDeclaration component) {
    try {
      return withInternalDeclarationSession("getSampleData()", session -> session.getSampleData(component));
    } catch (DeploymentStartException e) {
      String message = format("Couldn't start configuration(s) while retrieving sample data on component: '%s:%s'",
                              component.getDeclaringExtension(),
                              component.getName());
      LOGGER.error(message, e);
      return SampleDataResult.resultFrom(SampleDataFailure.Builder.newFailure(e)
          .withMessage(message)
          .withReason(getRootCauseMessage(e))
          .build());
    } catch (Throwable t) {
      LOGGER.error(format("Error while retrieving sample data on component: '%s:%s'", component.getDeclaringExtension(),
                          component.getName()),
                   t);
      throw t;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
  }

}
