/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.datatype;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.UnsupportedCharsetException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DataTypeTransformersTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private StringToCharsetTransformer toCharsetTransformer;
  private StringToMediaTypeTransformer toMediaTypeTransformer;

  @Before
  public void before() {
    toCharsetTransformer = new StringToCharsetTransformer();
    toCharsetTransformer.setEncodingSupplier(() -> UTF_8);
    toMediaTypeTransformer = new StringToMediaTypeTransformer();
    toMediaTypeTransformer.setEncodingSupplier(() -> UTF_8);
  }

  @Test
  public void validCharset() throws TransformerException {
    assertThat(toCharsetTransformer.transform(US_ASCII.name()), is(US_ASCII));
  }

  @Test
  public void invalidCharset() throws TransformerException {
    expected.expect(TransformerException.class);
    expected.expectCause(instanceOf(UnsupportedCharsetException.class));
    toCharsetTransformer.transform("invalid");
  }

  @Test
  public void validMediaType() throws TransformerException {
    final MediaType transformed = (MediaType) toMediaTypeTransformer.transform("text/plain");
    assertThat(transformed.getPrimaryType(), is("text"));
    assertThat(transformed.getSubType(), is("plain"));
    assertThat(transformed.getCharset().isPresent(), is(false));
  }

  @Test
  public void validMediaTypeWithCharset() throws TransformerException {
    final MediaType transformed = (MediaType) toMediaTypeTransformer.transform("text/plain;charset=" + US_ASCII.name());
    assertThat(transformed.getPrimaryType(), is("text"));
    assertThat(transformed.getSubType(), is("plain"));
    assertThat(transformed.getCharset().get(), is(US_ASCII));
  }

  @Test
  public void invalidMediaType() throws TransformerException {
    expected.expect(TransformerException.class);
    expected.expectCause(instanceOf(IllegalArgumentException.class));
    toMediaTypeTransformer.transform("invalid");
  }
}
