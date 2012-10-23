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

package net.dovemq.transport.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public class CAMQPSessionReceiverTest
{
    private Mockery mockContext = null;
    private ExecutorService executor = null;
    private static final AtomicInteger numLinkFlowFrameCount = new AtomicInteger(0);
    private CAMQPSession session = null;
    private final BlockingQueue<ChannelBuffer> incomingFrames = new LinkedBlockingQueue<ChannelBuffer>();
    private CAMQPConnection mockConnection = null;
    private long linkHandle = 5;
    
    private AtomicLong remoteWindow = new AtomicLong(256);
    
    static MockLinkReceiver linkReceiver = null;
    
    @BeforeClass
    public static void setupBeforeClass()
    {
        CAMQPSessionManager.registerLinkReceiverFactory(new MockLinkReceiverFactory());
    }
    
    @Before
    public void setup()
    {
        mockContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        executor = Executors.newScheduledThreadPool(2);
        numLinkFlowFrameCount.set(0);
        
        final CAMQPSessionStateActor mockStateActor = mockContext.mock(CAMQPSessionStateActor.class);
        mockConnection =  createMockConnection();
        session = new CAMQPSession(mockConnection, mockStateActor);
        session.retrieveAndSetRemoteFlowControlAttributes(CAMQPSessionConstants.DEFAULT_OUTGOING_WINDOW_SIZE, 0, CAMQPSessionConstants.DEFAULT_INCOMING_WINDOW_SIZE);
        
        mockContext.checking(new Expectations()
            {{
                ignoring(mockStateActor).getCurrentState();will(returnValue(State.MAPPED));
            }}
        );
    }

    @After
    public void tearDown()
    {
        executor.shutdown();
        mockContext.assertIsSatisfied();
        session = null;
        mockConnection = null;
        incomingFrames.clear();
        linkReceiver = null;
    }
    
    @AfterClass
    public static void cleanup()
    {
        CAMQPSessionManager.shutdown();
    }
    
    @Test(timeout=120000L)
    public void testSessionReceiveLinkControlFrame()
    {
        sendAttachedFrame();

        CAMQPControlDetach detachControl = new CAMQPControlDetach();
        detachControl.setHandle(linkHandle);
        detachControl.setClosed(true);

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlDetach.encode(encoder, detachControl);
        sendFrame(encoder, 1);

        assertEquals(true, linkReceiver.attachReceived);
        assertEquals(true, linkReceiver.detachReceived);
    }
    
    @Test(timeout=120000L)
    public void testSessionReceiveTransferFrames()
    {
        sendAttachedFrame();
        
        CAMQPControlTransfer transfer = new CAMQPControlTransfer();
        transfer.setHandle(linkHandle);
        transfer.setDeliveryId(0L);
        
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(encoder, transfer);
        encoder.writePayload(new CAMQPMessagePayload(new byte[1024]));
        sendFrame(encoder, 1);
    }
    
    @Test(timeout=120000L)
    public void testSessionReceiveLinkFlowFrame()
    {
        sendAttachedFrame();
        
        CAMQPControlTransfer transfer = new CAMQPControlTransfer();
        transfer.setHandle(linkHandle);
        transfer.setDeliveryId(0L);
        
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(encoder, transfer);
        encoder.writePayload(new CAMQPMessagePayload(new byte[1024]));
        sendFrame(encoder, 1);
        
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setOutgoingWindow(256L);
        flow.setIncomingWindow(256L);
        flow.setHandle(linkHandle);
        CAMQPEncoder encoder2 = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder2, flow);
        sendFrame(encoder2, 1);
        assertEquals(true, linkReceiver.linkFlowFrameReceived);
    }
    
    @Test(timeout=120000L)
    public void testSessionReceiveTransferFramesInduceFlowControl() throws InterruptedException
    {
        sendAttachedFrame();
        for (long i = 0; i < 275; i++)
        {
            CAMQPControlTransfer transfer = new CAMQPControlTransfer();
            transfer.setHandle(linkHandle);
            transfer.setDeliveryId(i);

            CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
            CAMQPControlTransfer.encode(encoder, transfer);
            encoder.writePayload(new CAMQPMessagePayload(new byte[1024]));
            sendFrame(encoder, 1);
        }
        
        getAndAssertFlowFrames();
    }
    
    private static class TransferFrameSender implements Runnable
    {
        private int numTransferFramesToSend;
        private final CAMQPSessionReceiverTest linkLayer;
        
        public TransferFrameSender(int numTransferFramesToSend, CAMQPSessionReceiverTest linkLayer)
        {
            super();
            this.numTransferFramesToSend = numTransferFramesToSend;
            this.linkLayer = linkLayer;
        }

        @Override
        public void run()
        {
            Random r = new Random();
            long nextTransferIdToSend = 0;
            
            while (numTransferFramesToSend > 0)
            {
                long remoteWindow = linkLayer.remoteWindow.getAndDecrement();
                while (remoteWindow <= 0)
                {
                    linkLayer.sendFlowFrame(1, true);
                    try
                    {
                        Thread.sleep(r.nextInt(500) + 500);
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    remoteWindow = linkLayer.remoteWindow.getAndDecrement();
                }
                linkLayer.sendTransferFrame(nextTransferIdToSend++, 1);
                numTransferFramesToSend--;
            }
        }       
    }
    
    @Test(timeout=120000L)
    public void testSessionReceiveTransferFramesInduceFlowControl2() throws InterruptedException
    {
        int numTransfers = 10000;
        Random r = new Random();
        remoteWindow.set(256);
        
        sendAttachedFrame();
        
        executor.submit(linkReceiver);
        
        executor.submit(new TransferFrameSender(numTransfers, this));
        
        Date lastFrameReceivedTime = new Date();
        while (true)
        {
            ChannelBuffer buffer = incomingFrames.poll(500, TimeUnit.MILLISECONDS);
            if (buffer == null)
            {
                Date now = new Date();
                if ((now.getTime() - lastFrameReceivedTime.getTime()) > 8000)
                {
                    break; // allow test to fail instead of hang
                }
                else
                    continue;
            }
            lastFrameReceivedTime = new Date();
            CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
            inputPipe.take(buffer);
        
            String controlName = inputPipe.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor))
            {
                CAMQPControlFlow outputData = CAMQPControlFlow.decode(inputPipe);
                assertTrue(!outputData.isSetHandle());
                
                remoteWindow.set(outputData.getIncomingWindow());
                if (outputData.isSetEcho() && outputData.getEcho())
                {
                    Thread.sleep(r.nextInt(500));                  
                    sendFlowFrame(1, false);
                }
            }
        }
        linkReceiver.shutdown = true;
        assertEquals(numTransfers-1, linkReceiver.lastTransferIdReceived);
    }
    
    private void sendTransferFrame(long transferId, int channelId)
    {
        CAMQPControlTransfer transfer = new CAMQPControlTransfer();
        transfer.setHandle(linkHandle);
        transfer.setDeliveryId(transferId);

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(encoder, transfer);
        encoder.writePayload(new CAMQPMessagePayload(new byte[1024]));
        sendFrame(encoder, channelId);
    }
    
    private void sendFlowFrame(int channelId, boolean setEchoFlag)
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setOutgoingWindow(256L);
        flow.setIncomingWindow(256L);
        if (setEchoFlag)
            flow.setEcho(true);
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder, flow);
        sendFrame(encoder, channelId);
    }
    
    private CAMQPConnection createMockConnection()
    {
        return new CAMQPConnection() {
            @Override
            public void sendFrame(ChannelBuffer buffer, int channelId)
            {
                incomingFrames.add(buffer);
            }
        };
    }
    
    private void sendAttachedFrame()
    {
        CAMQPControlAttach attachControl = new CAMQPControlAttach();
        attachControl.setHandle(linkHandle);
        attachControl.setName("TestLink");
        attachControl.setMaxMessageSize(BigInteger.valueOf(16384L));

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlAttach.encode(encoder, attachControl);
        sendFrame(encoder, 1);
    }
    
    private void sendFrame(CAMQPEncoder encoder, int channelId)
    {
        ChannelBuffer frameBody = encoder.getEncodedBuffer();
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(channelId, frameBody.readableBytes());
        session.frameReceived(new CAMQPFrame(frameHeader, frameBody));
    }
    
    private void getAndAssertFlowFrames() throws InterruptedException
    {
        boolean acked = false;
        Date lastFrameReceivedTime = new Date();
        while (true)
        {
            ChannelBuffer buffer = incomingFrames.poll(500, TimeUnit.MILLISECONDS);
            if (buffer == null)
            {
                Date now = new Date();
                if ((now.getTime() - lastFrameReceivedTime.getTime()) > 3000)
                {
                    break; // allow test to fail instead of hang
                }
                else
                    continue;
            }
            lastFrameReceivedTime = new Date();
            CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
            inputPipe.take(buffer);
        
            String controlName = inputPipe.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor))
            {
                CAMQPControlFlow outputData = CAMQPControlFlow.decode(inputPipe);
                assertTrue(!outputData.isSetHandle());
                assertTrue(outputData.isSetEcho());
                assertTrue(outputData.getEcho());
                if (!acked)
                    assertTrue(outputData.getIncomingWindow() < CAMQPSessionConstants.MIN_INCOMING_WINDOW_SIZE_THRESHOLD);
                if ((outputData.getIncomingWindow() == 0) && !acked)
                {
                    linkReceiver.ackTransfers(0, 275);
                    acked = true;
                    
                    Thread.sleep(1000);
                    
                    CAMQPControlFlow flow = new CAMQPControlFlow();
                    flow.setOutgoingWindow(256L);
                    flow.setIncomingWindow(256L);
                    CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
                    CAMQPControlFlow.encode(encoder, flow);
                    sendFrame(encoder, 1);
                }
            }
        }
    }
}
