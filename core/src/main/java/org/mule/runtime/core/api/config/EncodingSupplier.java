/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import java.nio.charset.Charset;
import java.util.function.Supplier;

/**
 * Allows to obtain the encoding to use in the current artifact.
 * 
 * @since 4.5
 */
public interface EncodingSupplier extends Supplier<Charset> {

  /**
   * @return the configured default encoding, checking in the following order until a value is found:
   *         <ul>
   *         <li>{@code muleContext} -> {@link org.mule.runtime.core.api.config.MuleConfiguration#getDefaultEncoding()}</li>
   *         <li>The value of the system property 'mule.encoding'</li>
   *         <li>{@code Charset.defaultCharset()}</li>
   *         </ul>
   */
  @Override
  Charset get();
}
