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

import java.util.Map;
import java.util.TreeMap;

public class ObjectToStringWithMapTestCase extends AbstractTransformerTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    ObjectToString transformer = new ObjectToString();
    transformer.setEncodingSupplier(() -> UTF_8);
    return transformer;
  }

  @Override
  public Object getTestData() {
    // TreeMap guarantees the order of keys. This is important for creating a test result
    // that is guaranteed to be comparable to the output of getResultData.
    Map map = new TreeMap();
    map.put("existingValue", "VALUE");
    map.put("nonexistingValue", null);
    return map;
  }

  @Override
  public Object getResultData() {
    return "{existingValue=VALUE, nonexistingValue=null}";
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    // we do not want round trip transforming tested
    return null;
  }

}


