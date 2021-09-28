/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.privileged.transformer.simple.SerialisedObjectTransformersTestCase;

public class ObjectByteArrayTransformersWithObjectsTestCase extends SerialisedObjectTransformersTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    ObjectToByteArray transformer = new ObjectToByteArray();
    transformer.setEncodingSupplier(() -> UTF_8);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transformer.hashCode()), transformer);
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    ByteArrayToObject transformer = new ByteArrayToObject();
    transformer.setEncodingSupplier(() -> UTF_8);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transformer.hashCode()), transformer);
    return transformer;
  }

}
