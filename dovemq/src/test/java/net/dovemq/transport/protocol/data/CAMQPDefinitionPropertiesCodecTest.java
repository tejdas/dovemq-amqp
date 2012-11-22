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

package net.dovemq.transport.protocol.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import net.dovemq.testutils.CAMQPTestUtils;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

public class CAMQPDefinitionPropertiesCodecTest
{
    @Test
    public void testCAMQPDefinitionProperties() throws Exception
    {
        CAMQPDefinitionProperties data = new CAMQPDefinitionProperties();

        Date now = new Date();

        data.setAbsoluteExpiryTime(now);
        data.setContentEncoding("application/json");
        data.setContentType("ascii");
        data.setCorrelationId("correlId-87877-binvalue".getBytes());
        data.setMessageId(BigInteger.valueOf(7235167L));
        data.setCreationTime(now);
        data.setGroupId("123456");
        data.setGroupSequence(87556L);
        data.setReplyTo("replyTo");
        data.setReplyToGroupId("76543L");
        data.setSubject("someSub");
        data.setTo("to");
        data.setUserId("userId".getBytes());

        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionProperties.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionProperties.descriptor));
        CAMQPDefinitionProperties outputData = CAMQPDefinitionProperties.decode(inputPipe);

        assertTrue(data.getAbsoluteExpiryTime().getTime() == outputData.getAbsoluteExpiryTime().getTime());
        assertTrue(data.getContentEncoding().equals(outputData.getContentEncoding()));
        assertTrue(data.getContentType().equals(outputData.getContentType()));
        CAMQPTestUtils.compateByteArrayObjects(data.getCorrelationId(), outputData.getCorrelationId());
        CAMQPTestUtils.compateBigIntegerObjects(data.getMessageId(), outputData.getMessageId());
        assertTrue(data.getCreationTime().getTime() == outputData.getCreationTime().getTime());
        assertTrue(data.getGroupId().equals(outputData.getGroupId()));
        assertEquals(data.getGroupSequence(), outputData.getGroupSequence());
        CAMQPTestUtils.compareStringObjects(data.getReplyTo(), outputData.getReplyTo());
        assertTrue(data.getReplyToGroupId().equals(outputData.getReplyToGroupId()));
        assertTrue(data.getSubject().equals(outputData.getSubject()));
        CAMQPTestUtils.compareStringObjects(data.getTo(), outputData.getTo());
        CAMQPTestUtils.compateByteArrays(data.getUserId(), outputData.getUserId());
    }

    @Test
    public void testCAMQPDefinitionPropertiesUUIDMessageId() throws Exception
    {
        CAMQPDefinitionProperties data = new CAMQPDefinitionProperties();

        Date now = new Date();

        data.setAbsoluteExpiryTime(now);
        data.setContentEncoding("application/json");
        data.setContentType("ascii");
        data.setCorrelationId("correlId-87877-binvalue".getBytes());
        UUID uuid = UUID.randomUUID();
        data.setMessageId(uuid);
        data.setCreationTime(now);
        data.setGroupId("123456");
        data.setGroupSequence(87556L);
        data.setReplyTo("replyTo");
        data.setReplyToGroupId("76543L");
        data.setSubject("someSub");
        data.setTo("to");
        data.setUserId("userId".getBytes());

        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionProperties.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionProperties.descriptor));
        CAMQPDefinitionProperties outputData = CAMQPDefinitionProperties.decode(inputPipe);

        assertTrue(data.getAbsoluteExpiryTime().getTime() == outputData.getAbsoluteExpiryTime().getTime());
        assertTrue(data.getContentEncoding().equals(outputData.getContentEncoding()));
        assertTrue(data.getContentType().equals(outputData.getContentType()));
        CAMQPTestUtils.compateByteArrayObjects(data.getCorrelationId(), outputData.getCorrelationId());
        CAMQPTestUtils.compareUUIDObjects(data.getMessageId(), outputData.getMessageId());
        assertTrue(data.getCreationTime().getTime() == outputData.getCreationTime().getTime());
        assertTrue(data.getGroupId().equals(outputData.getGroupId()));
        assertEquals(data.getGroupSequence(), outputData.getGroupSequence());
        CAMQPTestUtils.compareStringObjects(data.getReplyTo(), outputData.getReplyTo());
        assertTrue(data.getReplyToGroupId().equals(outputData.getReplyToGroupId()));
        assertTrue(data.getSubject().equals(outputData.getSubject()));
        CAMQPTestUtils.compareStringObjects(data.getTo(), outputData.getTo());
        CAMQPTestUtils.compateByteArrays(data.getUserId(), outputData.getUserId());
    }

    @Test
    public void testCAMQPDefinitionPropertiesMessageId() throws Exception
    {
        CAMQPDefinitionProperties data = new CAMQPDefinitionProperties();

        Date now = new Date();

        data.setAbsoluteExpiryTime(now);
        data.setContentEncoding("application/json");
        data.setContentType("ascii");
        data.setCorrelationId("correlId-87877-binvalue".getBytes());
        UUID uuid = UUID.randomUUID();
        data.setMessageId(uuid.toString());
        data.setCreationTime(now);
        data.setGroupId("123456");
        data.setGroupSequence(87556L);
        data.setReplyTo("replyTo");
        data.setReplyToGroupId("76543L");
        data.setSubject("someSub");
        data.setTo("to");
        data.setUserId("userId".getBytes());

        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionProperties.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionProperties.descriptor));
        CAMQPDefinitionProperties outputData = CAMQPDefinitionProperties.decode(inputPipe);

        assertTrue(data.getAbsoluteExpiryTime().getTime() == outputData.getAbsoluteExpiryTime().getTime());
        assertTrue(data.getContentEncoding().equals(outputData.getContentEncoding()));
        assertTrue(data.getContentType().equals(outputData.getContentType()));
        CAMQPTestUtils.compateByteArrayObjects(data.getCorrelationId(), outputData.getCorrelationId());
        CAMQPTestUtils.compareStringObjects(data.getMessageId(), outputData.getMessageId());
        assertTrue(data.getCreationTime().getTime() == outputData.getCreationTime().getTime());
        assertTrue(data.getGroupId().equals(outputData.getGroupId()));
        assertEquals(data.getGroupSequence(), outputData.getGroupSequence());
        CAMQPTestUtils.compareStringObjects(data.getReplyTo(), outputData.getReplyTo());
        assertTrue(data.getReplyToGroupId().equals(outputData.getReplyToGroupId()));
        assertTrue(data.getSubject().equals(outputData.getSubject()));
        CAMQPTestUtils.compareStringObjects(data.getTo(), outputData.getTo());
        CAMQPTestUtils.compateByteArrays(data.getUserId(), outputData.getUserId());
    }
}
