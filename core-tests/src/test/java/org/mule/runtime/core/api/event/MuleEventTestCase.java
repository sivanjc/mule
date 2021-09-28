/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.test.allure.AllureConstants.MuleEvent.MULE_EVENT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(MULE_EVENT)
public class MuleEventTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void teardown() {
    currentMuleContext.set(null);
  }

  @Test
  public void testFlowVarNamesAddImmutable() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    expectedException.expect(UnsupportedOperationException.class);
    event.getVariables().keySet().add("other");
  }

  @Test
  public void testFlowVarNamesRemoveImmutable() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    event = CoreEvent.builder(event).addVariable("test", "val").build();
    expectedException.expect(UnsupportedOperationException.class);
    event.getVariables().keySet().remove("test");
  }

  @Test
  public void testFlowVarsNotShared() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    event = CoreEvent.builder(event).addVariable("foo", "bar").build();

    CoreEvent copy = CoreEvent.builder(event).build();

    copy = CoreEvent.builder(copy).addVariable("foo", "bar2").build();

    assertEquals("bar", event.getVariables().get("foo").getValue());

    assertEquals("bar2", copy.getVariables().get("foo").getValue());
  }

  @Test
  @Description("Test that a perfromance optimization to avoid recreating the variables map is applied")
  public void varsOverridenFromAnotherEvent() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    CoreEvent baseEventNoVars = getEventBuilder()
        .message(of("whatever"))
        .build();

    final Map<String, TypedValue<?>> baseEventVars = baseEventWithVars.getVariables();

    final PrivilegedEvent newEvent = PrivilegedEvent.builder(baseEventNoVars).variablesTyped(baseEventVars).build();

    assertThat(newEvent.getVariables(), sameInstance(baseEventVars));
  }

  @Test
  @Description("Test that a performance optimization to avoid recreating the variables map is applied")
  public void varsOverridenFromAnotherEventNotEmpty() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    CoreEvent baseEventWithOtherVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("baz", "qux")
        .build();

    final Map<String, TypedValue<?>> baseEventVars = baseEventWithVars.getVariables();

    final PrivilegedEvent newEvent = PrivilegedEvent.builder(baseEventWithOtherVars).variablesTyped(baseEventVars).build();

    assertThat(newEvent.getVariables(), sameInstance(baseEventVars));
  }

  @Test
  public void varsCleared() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();

    final CoreEvent newEvent = CoreEvent.builder(baseEventWithVars).clearVariables().build();

    assertThat(newEvent.getVariables().isEmpty(), is(true));
  }

  @Test
  public void varsClearedAndAdded() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();

    String key = "survivor";
    String value = "Tom Hanks";
    final CoreEvent newEvent = CoreEvent.builder(baseEventWithVars)
        .clearVariables()
        .addVariable(key, value)
        .build();

    assertThat(newEvent.getVariables().size(), is(1));

    TypedValue<?> actual = newEvent.getVariables().get(key);
    assertThat(actual.getValue(), is(value));
  }

  @Test
  public void securityContextCopy() throws Exception {
    SecurityContext securityContext = mock(SecurityContext.class);
    CoreEvent event = CoreEvent.builder(testEvent()).securityContext(securityContext).build();

    CoreEvent eventCopy = CoreEvent.builder(event).message(Message.of("copy")).build();

    assertThat(securityContext, sameInstance(eventCopy.getSecurityContext()));
  }

  @Test
  @Issue("MULE-18157")
  public void securityContextNull() throws Exception {
    CoreEvent event = CoreEvent.builder(testEvent()).securityContext(null).build();

    CoreEvent eventCopy = CoreEvent.builder(event).message(Message.of("copy")).securityContext(null).build();

    assertThat(eventCopy.getMessage().getPayload().getValue(), is("copy"));
  }

  @Test
  @Description("Validates that the correlation IDs are unique")
  @Issue("MULE-17926")
  public void uniqueCorrelationIDs() throws MuleException {
    CoreEvent firstEvent = getEventBuilder().message(of("first")).build();
    CoreEvent secondEvent = getEventBuilder().message(of("second")).build();

    assertThat("Duplicated correlationID", firstEvent.getContext().getCorrelationId(),
               not(is(secondEvent.getContext().getCorrelationId())));
  }

}
