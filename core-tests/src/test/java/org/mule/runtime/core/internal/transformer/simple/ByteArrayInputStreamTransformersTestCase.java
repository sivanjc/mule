/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;

public class ByteArrayInputStreamTransformersTestCase extends AbstractTransformerTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    ObjectToInputStream transformer = new ObjectToInputStream();
    transformer.setEncodingSupplier(() -> UTF_8);
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    ObjectToByteArray transformer = new ObjectToByteArray();
    transformer.setEncodingSupplier(() -> UTF_8);
    return transformer;
  }

  @Override
  public Object getTestData() {
    return TEST_MESSAGE.getBytes();
  }

  @Override
  public Object getResultData() {
    return new ByteArrayInputStream(TEST_MESSAGE.getBytes());
  }

}
