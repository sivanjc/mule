/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.IOUtils;
import org.mule.util.StringDataSource;

import javax.activation.DataHandler;

public class HttpAttachmentsFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "http-attachments-functional-test.xml";
    }

    public void testSendAttachment() throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback(){
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                assertEquals("application/octet-stream; charset=ISO-8859-1", context.getMessage().getInboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY));
                assertEquals("We should have an attachment", 1, context.getMessage().getInboundAttachmentNames().size());
                DataHandler dh = context.getMessage().getInboundAttachment("attach1");
                assertNotNull("DataHandler with name 'attach1' should not be null", dh);
                assertEquals("We should have an attachment with foo", "foo" , IOUtils.toString(dh.getInputStream()));
                assertEquals("text/plain; charset=ISO-8859-1", dh.getContentType());
            }
        });

        LocalMuleClient client = muleContext.getClient();
        MuleMessage msg = new DefaultMuleMessage("test",  muleContext);
        msg.addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));

        MuleMessage result = client.send("endpoint1", msg);
        assertEquals("We should have no attachments coming back", 0, result.getInboundAttachmentNames().size());
    }


    //TODO MULE-5005 response attachments
//    public void testReceiveAttachment() throws Exception
//    {
//        FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
//        assertNotNull(ftc);
//        ftc.setEventCallback(new EventCallback(){
//            public void eventReceived(MuleEventContext context, Object component) throws Exception
//            {
//                context.getMessage().addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));
//            }
//        });
//
//        LocalMuleClient client = muleContext.getClient();
//        MuleMessage msg = new DefaultMuleMessage("test",  muleContext);
//
//        //msg.addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));
//
//        MuleMessage result = client.send("endpoint1", msg);
//        assertEquals("We should have 1 attachments coming back", 1, result.getInboundAttachmentNames().size());
//        assertEquals("There should be no outbound attachments", 0, result.getOutboundAttachmentNames().size());
//        DataHandler dh = result.getInboundAttachment("attach1");
//        assertNotNull("DataHandler with name 'attach1' should not be null", dh);
//        assertEquals("We should have an attachment with foo", "foo" , IOUtils.toString(dh.getInputStream()));
//        assertEquals("text/plain", dh.getContentType());
//    }

}
