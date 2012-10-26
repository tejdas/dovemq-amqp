/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;

import org.apache.commons.lang.RandomStringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

public class DoveMQMessageCodecTest extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testMessageCodec()
    {
        messageCodecTest(true, true, true, true, 3);
        messageCodecTest(true, true, true, true, 2);
        messageCodecTest(true, true, true, true, 1);
        messageCodecTest(true, true, true, true, 0);
        messageCodecTest(true, false, true, true, 3);
        messageCodecTest(false, true, true, true, 2);
        messageCodecTest(true, true, false, true, 1);
        messageCodecTest(true, true, true, false, 0);
        messageCodecTest(false, false, false, false, 0);
        messageCodecTest(false, false, false, false, 4);
        messageCodecTest(false, true, false, true, 1);
        messageCodecTest(true, false, true, false, 1);
    }

    private void messageCodecTest(boolean setAppProperty, boolean setDeliveryAnnotation, boolean setMessageAnnotation,
            boolean setFooter, int messageCount)
    {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();

        DoveMQMessageImpl message = new DoveMQMessageImpl();
        if (setAppProperty)
            message.addApplicationProperty("appPropKey", "appPropVal");

        if (setDeliveryAnnotation)
            message.addDeliveryAnnotation("deliveryKey", "deliveryVal");

        if (setFooter)
            message.addFooter("footerkey", "footerval");

        if (setMessageAnnotation)
            message.addMessageAnnotation("messageAnnotKey", "messageAnnotVal");

        List<String> inPayloads = new ArrayList<String>();
        for (int i = 0; i < messageCount; i++)
        {
            String payload = RandomStringUtils.randomAlphanumeric(1024);
            inPayloads.add(payload);
            byte[] payloadBytes = payload.getBytes();
            message.addPayload(payloadBytes);
        }

        message.encode(encoder);
        ChannelBuffer buffer = encoder.getEncodedBuffer();

        CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        DoveMQMessageImpl outMessage = DoveMQMessageImpl.decode(inputPipe);

        if (setAppProperty)
            assertTrue("appPropVal".equals(outMessage.getApplicationProperty("appPropKey")));

        if (setDeliveryAnnotation)
            assertTrue("deliveryVal".equals(outMessage.getDeliveryAnnotation("deliveryKey")));

        if (setMessageAnnotation)
            assertTrue("messageAnnotVal".equals(outMessage.getMessageAnnotation("messageAnnotKey")));

        if (setFooter)
            assertTrue("footerval".equals(outMessage.getFooter("footerkey")));

        Collection<byte[]> payloads = outMessage.getPayloads();
        if (messageCount == 0)
            assertTrue(payloads == null);
        else
        {
            assertTrue(payloads.size() == messageCount);
            List<String> outPayloads = new ArrayList<String>();

            for (byte[] payload : payloads)
            {
                outPayloads.add(new String(payload));
            }
            assertTrue(outPayloads.containsAll(inPayloads));
        }
    }
}

