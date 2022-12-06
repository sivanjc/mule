/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.junit.Test;
import org.mule.test.petstore.extension.PetStoreClient;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class ModuleWithImplicitConfigurationWithExpressionsTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params-expression.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-without-config.xml";
  }

  @Test
  public void testDoGetClient() throws Exception {
    assertGetClient("testDoGetClient");
  }

  @Test
  public void testDoGetUsername() throws Exception {
    String result = (String) flowRunner("testDoGetUsername")
        .run().getMessage().getPayload().getValue();
    assertThat(result, is("john"));
  }

  private void assertGetClient(String flow) throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner(flow)
        .run().getMessage().getPayload().getValue();
    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("notDoe"));
  }

}
