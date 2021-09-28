/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.message;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase {

  public static final Charset DEFAULT_ENCODING = UTF_8;
  public static final Charset CUSTOM_ENCODING = UTF_16;
  public static final String TEST_PROPERTY = "testProperty";
  public static final MediaType APPLICATION_XML_DEFAULT = APPLICATION_XML.withCharset(DEFAULT_ENCODING);
  public static final MediaType APPLICATION_XML_CUSTOM = APPLICATION_XML.withCharset(CUSTOM_ENCODING);

  private final MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class, RETURNS_DEEP_STUBS);
  private ExtendedTransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING.name());
    transformationService = new ExtendedTransformationService(muleContext, mock(DataTypeConversionResolver.class), () -> UTF_8);
  }

  @Test
  public void updatesTypeOnTransformation() throws Exception {
    Message message = Message.builder().value(1).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(ANY).charset(DEFAULT_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(1);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
  }

  @Test
  public void updatesEncodingOnTransformation() throws Exception {
    Message message = Message.builder().value(TEST_PAYLOAD).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).charset(CUSTOM_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(Integer.valueOf(1));

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesMimeTypeOnTransformation() throws Exception {
    Message message = Message.builder().value(TEST_PAYLOAD).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(APPLICATION_XML).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(any(Message.class))).thenReturn(1);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void maintainsCurrentDataTypeClassWhenTransformerOutputTypeIsObject() throws Exception {
    Message message = of(TEST_PAYLOAD);

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Object.class).mediaType(ANY).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(message)).thenReturn(TEST_PAYLOAD);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, String.class, ANY, DEFAULT_ENCODING);
  }

  private void assertDataType(Message muleMessage, Class<?> type, MediaType mimeType, Charset encoding) {
    assertThat(muleMessage.getPayload().getDataType(), like(type, mimeType, encoding));
  }

}
