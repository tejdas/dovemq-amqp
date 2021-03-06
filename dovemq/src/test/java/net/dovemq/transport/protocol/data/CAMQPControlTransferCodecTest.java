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

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import junit.framework.TestCase;
import net.dovemq.testutils.CAMQPTestUtils;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

public class CAMQPControlTransferCodecTest extends TestCase
{

    public CAMQPControlTransferCodecTest(String name)
    {
        super(name);
    }

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
    public void testCAMQPControlTransferCodec() throws Exception
    {
        CAMQPControlTransfer data = new CAMQPControlTransfer();

        data.setAborted(true);
        data.setBatchable(false);
        String foo = "Special-Message";
        data.setDeliveryTag(foo.getBytes());
        data.setHandle(423L);
        data.setMore(true);
        data.setResume(false);
        data.setSettled(true);

        CAMQPDefinitionDeliveryState xferState = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        xferState.setSectionOffset(BigInteger.valueOf(bytesTransferred));
        CAMQPDefinitionModified outcomeVal = new CAMQPDefinitionModified();
        outcomeVal.setUndeliverableHere(false);
        outcomeVal.setDeliveryFailed(false);

        outcomeVal.setRequiredMessageAttrs(true);
        outcomeVal.getMessageAttrs().put("msgkey", "msgval");
        outcomeVal.getMessageAttrs().put("msgkey2", "msgval2");
        outcomeVal.getMessageAttrs().put("msgkey3", "msgval2");

        xferState.setOutcome(outcomeVal);
        xferState.setTxnId("transactionid4");

        data.setState(xferState);

        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(outstream, data);
        String uuidstr = UUID.randomUUID().toString();

        CAMQPMessagePayload payload = new CAMQPMessagePayload(uuidstr.getBytes());
        outstream.writePayload(payload);
        ChannelBuffer buffer = outstream.getEncodedBuffer();

        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor));
        CAMQPControlTransfer outputData = CAMQPControlTransfer.decode(inputPipe);

        CAMQPMessagePayload outPayload = inputPipe.getPayload();

        CAMQPTestUtils.comparePayloads(payload, outPayload);

        assertTrue(outputData.getAborted() == data.getAborted());
        assertTrue(outputData.getBatchable() == data.getBatchable());

        CAMQPTestUtils.compateByteArrays(data.getDeliveryTag(), outputData.getDeliveryTag());
        assertEquals(outputData.getHandle(), data.getHandle());
        assertTrue(outputData.getMore() == data.getMore());
        assertTrue(outputData.getResume() == data.getResume());
        assertTrue(outputData.getSettled() == data.getSettled());

        CAMQPDefinitionDeliveryState xferStateIn = (CAMQPDefinitionDeliveryState) data.getState();
        CAMQPDefinitionDeliveryState xferStateOut = (CAMQPDefinitionDeliveryState) outputData.getState();

        assertEquals(xferStateIn.getSectionOffset().longValue(), xferStateOut.getSectionOffset().longValue());
        assertTrue(xferStateIn.getTxnId().equalsIgnoreCase(xferStateOut.getTxnId()));

        CAMQPDefinitionModified outComeInput = (CAMQPDefinitionModified) xferStateIn.getOutcome();
        CAMQPDefinitionModified outComeOutput = (CAMQPDefinitionModified) xferStateOut.getOutcome();

        assertTrue(outComeInput.getUndeliverableHere() == outComeOutput.getUndeliverableHere());
        assertTrue(outComeInput.getDeliveryFailed() == outComeOutput.getDeliveryFailed());

        {
            assertTrue(outComeOutput.getMessageAttrs().size() == outComeInput.getMessageAttrs().size());
            Map<String, String> map = outComeOutput.getMessageAttrs();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outComeInput.getMessageAttrs().get(s).equalsIgnoreCase(outComeOutput.getMessageAttrs().get(s)));
            }
        }
    }

    @Test
    public void testCAMQPControlTransferCodecBigPayload() throws Exception
    {
        CAMQPControlTransfer data = new CAMQPControlTransfer();

        data.setAborted(true);
        data.setBatchable(false);
        String foo = "Special-Message";
        data.setDeliveryTag(foo.getBytes());
        data.setHandle(423L);
        data.setMore(true);
        data.setResume(false);
        data.setSettled(true);

        CAMQPDefinitionDeliveryState xferState = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        xferState.setSectionOffset(BigInteger.valueOf(bytesTransferred));
        CAMQPDefinitionModified outcomeVal = new CAMQPDefinitionModified();
        outcomeVal.setUndeliverableHere(false);
        outcomeVal.setDeliveryFailed(false);

        outcomeVal.setRequiredMessageAttrs(true);
        outcomeVal.getMessageAttrs().put("msgkey", "msgval");
        outcomeVal.getMessageAttrs().put("msgkey2", "msgval2");
        outcomeVal.getMessageAttrs().put("msgkey3", "msgval2");

        xferState.setOutcome(outcomeVal);
        xferState.setTxnId("transactionid4");

        data.setState(xferState);

        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(outstream, data);

        byte[] inbuf = new byte[1048576];
        new Random().nextBytes(inbuf);
        CAMQPMessagePayload inputPayload = new CAMQPMessagePayload(inbuf);

        outstream.writePayload(inputPayload);
        ChannelBuffer buffer = outstream.getEncodedBuffer();

        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor));
        CAMQPControlTransfer outputData = CAMQPControlTransfer.decode(inputPipe);

        CAMQPMessagePayload outputPayload = inputPipe.getPayload();

        CAMQPTestUtils.comparePayloads(inputPayload, outputPayload);

        assertTrue(outputData.getAborted() == data.getAborted());
        assertTrue(outputData.getBatchable() == data.getBatchable());

        CAMQPTestUtils.compateByteArrays(data.getDeliveryTag(), outputData.getDeliveryTag());
        assertEquals(outputData.getHandle(), data.getHandle());
        assertTrue(outputData.getMore() == data.getMore());
        assertTrue(outputData.getResume() == data.getResume());
        assertTrue(outputData.getSettled() == data.getSettled());

        CAMQPDefinitionDeliveryState xferStateIn = (CAMQPDefinitionDeliveryState) data.getState();
        CAMQPDefinitionDeliveryState xferStateOut = (CAMQPDefinitionDeliveryState) outputData.getState();

        assertEquals(xferStateIn.getSectionOffset().longValue(), xferStateOut.getSectionOffset().longValue());
        assertTrue(xferStateIn.getTxnId().equalsIgnoreCase(xferStateOut.getTxnId()));

        CAMQPDefinitionModified outComeInput = (CAMQPDefinitionModified) xferStateIn.getOutcome();
        CAMQPDefinitionModified outComeOutput = (CAMQPDefinitionModified) xferStateOut.getOutcome();

        assertTrue(outComeInput.getUndeliverableHere() == outComeOutput.getUndeliverableHere());
        assertTrue(outComeInput.getDeliveryFailed() == outComeOutput.getDeliveryFailed());

        {
            assertTrue(outComeOutput.getMessageAttrs().size() == outComeInput.getMessageAttrs().size());
            Map<String, String> map = outComeOutput.getMessageAttrs();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outComeInput.getMessageAttrs().get(s).equalsIgnoreCase(outComeOutput.getMessageAttrs().get(s)));
            }
        }
    }
}
