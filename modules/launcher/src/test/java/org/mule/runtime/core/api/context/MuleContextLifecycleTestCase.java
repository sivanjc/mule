/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MuleContextLifecycleTestCase extends AbstractMuleTestCase {

  private MuleContextBuilder ctxBuilder;
  private MuleContext ctx;
  private NotificationListenerRegistry notificationListenerRegistry;

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  @Before
  public void setup() throws Exception {
    ctxBuilder = new DefaultMuleContextBuilder(APP);
    ctx = ctxBuilder.buildMuleContext();

    notificationListenerRegistry = new DefaultNotificationListenerRegistry();
    ((MuleContextWithRegistry) ctx).getRegistry().registerObject(NotificationListenerRegistry.REGISTRY_KEY,
                                                                 notificationListenerRegistry);
    testServicesConfigurationBuilder.configure(ctx);
  }

  @After
  public void tearDown() throws Exception {
    if (ctx != null && !ctx.isDisposed()) {
      ctx.dispose();
    }
  }

  @Test(expected = InitialisationException.class)
  public void testIsInValidJdk() throws InitialisationException {
    try {
      JdkVersionUtils.validateJdk();
    } catch (RuntimeException e) {
      fail("Jdk version or vendor is invalid. Update the valid versions");
    }

    String javaVersion = System.setProperty("java.version", "1.5.0_12");
    try {
      try {
        JdkVersionUtils.validateJdk();
        fail("Test is invalid because the Jdk version or vendor is supposed to now be invalid");
      } catch (RuntimeException e) {
        // expected
      }

      MuleContext ctx = ctxBuilder.buildMuleContext();
      assertFalse(ctx.isInitialised());
      assertFalse(ctx.isInitialising());
      assertFalse(ctx.isStarted());
      assertFalse(ctx.isDisposed());
      assertFalse(ctx.isDisposing());

      ctx.initialise();
    } finally {
      System.setProperty("java.version", javaVersion);
    }
  }
}
