/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

module org.mule.service.scheduler.mock {
 
  requires org.mule.runtime.api;
  requires org.mule.runtime.core;

  // Allow invocation and injection into providers by the Mule Runtime
  exports org.mule.service.scheduler to
      org.mule.runtime.service;

}