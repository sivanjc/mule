/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.apache.commons.lang3.SerializationUtils;

public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase {

  private final Orange testObject = new Orange(new Integer(4), new Double(14.3), "nice!");

  @Override
  public Transformer getTransformer() throws Exception {
    SerializableToByteArray transformer = new SerializableToByteArray();
    transformer.setEncodingSupplier(() -> UTF_8);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transformer.hashCode()), transformer);
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    ByteArrayToSerializable transformer = new ByteArrayToSerializable();
    transformer.setEncodingSupplier(() -> UTF_8);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transformer.hashCode()), transformer);
    return transformer;
  }

  @Override
  public Object getTestData() {
    return testObject;
  }

  @Override
  public Object getResultData() {
    return SerializationUtils.serialize(testObject);
  }

}
