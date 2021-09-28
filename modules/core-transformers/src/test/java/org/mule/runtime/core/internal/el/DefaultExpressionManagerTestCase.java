/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
public class DefaultExpressionManagerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamingManager streamingManager;

  private ExtendedExpressionManager expressionManager;

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = new DefaultExpressionManager();
    initialiseIfNeeded(expressionManager, muleContext);
  }

  @Test
  @Description("Verifies that XML content can be used for logging in DW.")
  public void parseLogXml() throws MuleException {
    CoreEvent event = getEventBuilder().message(Message.builder().value("<?xml version='1.0' encoding='US-ASCII'?>\n"
        + "<wsc_fields>\n"
        + "  <operation>echo</operation>\n"
        + "  <body_test>test</body_test>\n"
        + "</wsc_fields>")
        .mediaType(XML)
        .build())
        .build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload.wsc_fields.operation]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is \"echo\""));
  }

  @Test
  @Description("Verifies that streams are logged in DW but not in MVEL.")
  public void parseLogStream() throws MuleException {
    ByteArrayInputStream stream = new ByteArrayInputStream("hello".getBytes());
    CoreEvent event = getEventBuilder().message(Message.of(stream)).build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is hello"));
    assertThat(expressionManager.parseLogTemplate("this is #[mel:payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               both(startsWith("this is ")).and(containsString(stream.getClass().getSimpleName())));
  }

  @Test
  @Description("Verifies that streams are logged in DW but not in MVEL.")
  public void parseLogStreamDefault() throws MuleException {
    ByteArrayInputStream stream = new ByteArrayInputStream("hello".getBytes());
    CoreEvent event = getEventBuilder().message(of(stream)).build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is hello"));
  }


}
