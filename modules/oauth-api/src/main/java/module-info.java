/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule service that provides OAuth authentication support.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.oauth.api {

  requires transitive org.mule.runtime.api;
  requires org.mule.oauth.client.api;
  requires org.mule.runtime.http.api;

  exports org.mule.runtime.oauth.api;
  exports org.mule.runtime.oauth.api.builder;
  exports org.mule.runtime.oauth.api.listener;
  exports org.mule.runtime.oauth.api.state;
  
}
