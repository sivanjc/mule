/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.EncodingSupplier;

import java.nio.charset.Charset;

import javax.inject.Inject;

/**
 * Default implementation of {@link EncodingSupplier}.
 * 
 * @since 4.5
 */
public class DefaultEncodingSupplier implements EncodingSupplier {

  private MuleContext muleContext;

  @Override
  public Charset get() {
    if (muleContext != null && muleContext.getConfiguration().getDefaultEncoding() != null) {
      return Charset.forName(muleContext.getConfiguration().getDefaultEncoding());
    } else if (System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY) != null) {
      return Charset.forName(System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY));
    } else {
      return Charset.defaultCharset();
    }
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

}
