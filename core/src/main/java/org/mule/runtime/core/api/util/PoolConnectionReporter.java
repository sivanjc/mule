/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class PoolConnectionReporter {

  private static PoolConnectionReporter instance = new PoolConnectionReporter();
  private Map<String, Map<String, ConnectionPoolInfo>> connectionPoolsInfo = new HashMap<>();

  public static PoolConnectionReporter getInstance() {
    return instance;
  }

  public String getConnectionPoolInfo(String applicationName) {
    Gson gson = new Gson();
    if (this.connectionPoolsInfo.containsKey(applicationName)) {
      return gson.toJson(this.connectionPoolsInfo.get(applicationName));
    }
    return gson.toJson("Application not found");
  }

  public void addConnectionPoolInfo(String application, String configName, ConnectionPoolInfo info) {
    if (!connectionPoolsInfo.containsKey(application)) {
      connectionPoolsInfo.put(application, new HashMap<>());
    }
    connectionPoolsInfo.get(application).put(configName, info);
  }
}
