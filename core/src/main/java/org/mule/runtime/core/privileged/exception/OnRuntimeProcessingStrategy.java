/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategySupplier;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Optional.empty;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

public class OnRuntimeProcessingStrategy implements ProcessingStrategy {

  private final ConfigurationComponentLocator locator;

  private ThreadLocal<CoreEvent> coreEvent = new ThreadLocal<>();

  public OnRuntimeProcessingStrategy(ConfigurationComponentLocator locator) {
    this.locator = locator;
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    return null;
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return publisher -> Flux.from(publisher)
        .flatMap(e -> Mono.just(e).transform(getProcessingStrategy(e).get().onProcessor(processor)));
  }

  public Optional<ProcessingStrategy> getProcessingStrategy(CoreEvent coreEvent) {
    return MessageProcessors.getProcessingStrategy(locator, Location.builder()
        .globalName(coreEvent.getContext().getOriginatingLocation().getRootContainerName())
        .build());
  }

  private Consumer<? super CoreEvent> doSomething() {
    return e -> System.out.println("something");
  }

  private Optional<ProcessingStrategy> getProcessingStrategy(ConfigurationComponentLocator locator,
                                                             TypedComponentIdentifier componentIdentifier) {
    final List<Component> found = locator.find(componentIdentifier.getIdentifier());

    return Optional.of(found)
        .filter(loc -> loc instanceof ProcessingStrategySupplier)
        .map(loc -> ((ProcessingStrategySupplier) loc).getProcessingStrategy());
  }
}
