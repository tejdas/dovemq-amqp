package net.dovemq.transport.protocol.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

import net.dovemq.testutils.CAMQPTestUtils;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;

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
}
