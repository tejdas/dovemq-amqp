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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

class MockLinkSender implements CAMQPLinkSenderInterface
{
    @Override
    public void sendMessage(CAMQPMessage message)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void messageSent(CAMQPControlTransfer transferFrame)
    {
        // TODO Auto-generated method stub

    }
}

public class CAMQPSessionSenderTest
{
    private Mockery mockContext = null;
    private ExecutorService executor = null;
    private static final AtomicInteger numLinkFlowFrameCount = new AtomicInteger(0);
    private CAMQPSession session = null;
    private final BlockingQueue<ChannelBuffer> outgoingFrames = new LinkedBlockingQueue<ChannelBuffer>();
    private CAMQPConnection mockConnection = null;

    @BeforeClass
    public static void setupBeforeClass()
    {
    }

    @Before
    public void setup()
    {
        mockContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        executor = Executors.newFixedThreadPool(32);
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
        outgoingFrames.clear();
    }

    private class DummyLinkSource implements Runnable
    {
        private final CAMQPSession session;
        private final int numberOfTransfers;
        private final CountDownLatch latch;
        public DummyLinkSource(int numberOfTransfers, CAMQPSession session, CountDownLatch latch)
        {
            super();
            this.numberOfTransfers = numberOfTransfers;
            this.session = session;
            this.latch = latch;
        }

        @Override
        public void run()
        {
            long linkHandle = Thread.currentThread().getId();
            Random r = new Random(19580427);
            try
            {
                latch.await();
                int sleepTime = r.nextInt(50) + 5;
                Thread.sleep(r.nextInt(sleepTime));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            CAMQPLinkSenderInterface mockLinkSender = new MockLinkSender();

            try
            {
            for (int i = 0; i < numberOfTransfers; i++)
            {
                CAMQPControlTransfer transfer = new CAMQPControlTransfer();
                transfer.setDeliveryId(session.getNextDeliveryId());
                session.sendTransfer(transfer, new CAMQPMessagePayload(new byte[2048]), mockLinkSender);
                try
                {
                    int sleepTime = r.nextInt(25) + 5;
                    Thread.sleep(r.nextInt(sleepTime));
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }

                if (i%50 == 5)
                {
                    CAMQPControlFlow flowFrame = createLinkFlowFrame(linkHandle);
                    numLinkFlowFrameCount.incrementAndGet();
                    session.sendFlow(flowFrame);
                }
            }
            }
            catch (RuntimeException ex)
            {
                System.out.println(ex.toString());
                ex.printStackTrace();
            }
        }
    }

    @AfterClass
    public static void cleanup()
    {
        CAMQPSessionManager.shutdown();
    }

    @Test
    public void testSendLinkControlFrame() throws InterruptedException
    {
        long linkHandle = 5L;
        ChannelBuffer encodedFrame = createAttachFrame(linkHandle);
        session.sendLinkControlFrame(encodedFrame);

        ChannelBuffer buffer = outgoingFrames.poll(500, TimeUnit.MILLISECONDS);
        CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlAttach.descriptor));
        CAMQPControlAttach controlFrame = CAMQPControlAttach.decode(inputPipe);
        assertTrue(linkHandle == controlFrame.getHandle());
    }

    @Test
    public void testSession() throws InterruptedException
    {
        sendTransfers(1, 100);
    };

    @Test(timeout=120000L)
    public void testSessionSimulateFlowReceipt() throws InterruptedException
    {
        int numTransfersPerLink = 5000;
        CountDownLatch latch = new CountDownLatch(1);
        DummyLinkSource linkSource = new DummyLinkSource(numTransfersPerLink, session, latch);
        executor.submit(linkSource);

        latch.countDown();
        getAndAssertFlowFrames(0, numTransfersPerLink);
    };

    @Test(timeout=120000L)
    public void testSessionMultipleLinks() throws InterruptedException
    {
        int numLinks = 12;
        int numTransfersPerLink = 256;
        sendTransfers(numLinks, numTransfersPerLink);
    };

    private void sendTransfers(int numLinks, int numTransfersPerLink) throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < numLinks; i++)
        {
            DummyLinkSource linkSource = new DummyLinkSource(numTransfersPerLink, session, latch);
            executor.submit(linkSource);
        }

        latch.countDown();
        getAndAssertTransferCount(0, numTransfersPerLink * numLinks);
    };

    private void getAndAssertTransferCount(long initialTransferId, int expectedTransferFrames) throws InterruptedException
    {
        int numLinkFlowFrames = 0;
        long nextExpectedIncomingTransferId = initialTransferId;
        Date lastFrameReceivedTime = new Date();
        while (true)
        {
            ChannelBuffer buffer = outgoingFrames.poll(500, TimeUnit.MILLISECONDS);
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
            if (controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor))
            {
                CAMQPControlTransfer.decode(inputPipe);
                nextExpectedIncomingTransferId++;
            }
            else if (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor))
            {
                CAMQPControlFlow outputData = CAMQPControlFlow.decode(inputPipe);
                if (!outputData.isSetHandle())
                {
                    simulateFlowFrameFromSessionReceiver(session, nextExpectedIncomingTransferId, false);
                }
                else
                    numLinkFlowFrames++;
            }

            if (nextExpectedIncomingTransferId == expectedTransferFrames)
            {
                break;
            }
        }
        assertEquals(expectedTransferFrames, nextExpectedIncomingTransferId);
        assertEquals(numLinkFlowFrameCount.get(), numLinkFlowFrames);
    }

    private void getAndAssertFlowFrames(long initialTransferId, int expectedTransferFrames) throws InterruptedException
    {
        int numEchoFlowFramesSent = 0;
        int numEchoFlowFramesRecvd = 0;
        int numLinkFlowFrames = 0;
        long nextExpectedIncomingTransferId = initialTransferId;
        Date lastFrameReceivedTime = new Date();
        while (true)
        {
            ChannelBuffer buffer = outgoingFrames.poll(500, TimeUnit.MILLISECONDS);
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
            if (controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor))
            {
                CAMQPControlTransfer.decode(inputPipe);
                nextExpectedIncomingTransferId++;
            }
            else if (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor))
            {
                CAMQPControlFlow outputData = CAMQPControlFlow.decode(inputPipe);
                if (!outputData.isSetHandle())
                {
                    if (!outputData.getEcho())
                        numEchoFlowFramesRecvd++;
                    else
                    {
                        simulateFlowFrameFromSessionReceiver(session, nextExpectedIncomingTransferId, true);
                        numEchoFlowFramesSent++;
                    }
                }
                else
                    numLinkFlowFrames++;
            }

            if (nextExpectedIncomingTransferId == expectedTransferFrames)
            {
                if (numEchoFlowFramesSent == numEchoFlowFramesRecvd)
                    break;
            }
        }
        assertEquals(expectedTransferFrames, nextExpectedIncomingTransferId);
        assertEquals(numLinkFlowFrames, numLinkFlowFrameCount.get());
        assertEquals(numEchoFlowFramesSent, numEchoFlowFramesRecvd);
    }

    private void simulateFlowFrameFromSessionReceiver(CAMQPSession session, long sessionReceiverNextIncomingId, boolean echoFlag)
    {
        Random r = new Random();
        try
        {
            int sleepTime = r.nextInt(45) + 5;
            Thread.sleep(sleepTime);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        CAMQPControlFlow flow = new CAMQPControlFlow();
        long incomingWindow = (r.nextInt(64) + 192);
        flow.setIncomingWindow(incomingWindow);
        /*
         * set nextIncomingId in the flow-frame a little lower than sessionReceiverNextIncomingId
         * to simulate frames in the transit
         */
        int numFramesInTransit = r.nextInt(30);
        long nextIncomingId = sessionReceiverNextIncomingId - numFramesInTransit;
        flow.setNextIncomingId(nextIncomingId);
        flow.setEcho(echoFlag);

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder, flow);
        ChannelBuffer frameBody = encoder.getEncodedBuffer();
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(0, frameBody.readableBytes());
        session.frameReceived(new CAMQPFrame(frameHeader, frameBody));
    }

    private static CAMQPControlFlow createLinkFlowFrame(long linkHandle)
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setAvailable(25L);
        flow.setDrain(false);
        flow.setEcho(true);
        flow.setHandle(linkHandle);
        flow.setDeliveryCount(3256L);
        flow.setLinkCredit(50L);
        return flow;
    }

    private CAMQPConnection createMockConnection()
    {
        return new CAMQPConnection() {
            @Override
            public void sendFrame(ChannelBuffer buffer, int channelId)
            {
                outgoingFrames.add(buffer);
            }
        };
    }

    public static CAMQPSessionInterface createMockSessionAndSetExpectations(Mockery mockContext, CAMQPConnection mockConnection)
    {
        final CAMQPSessionStateActor mockStateActor = mockContext.mock(CAMQPSessionStateActor.class);
        CAMQPSession session = new CAMQPSession(mockConnection, mockStateActor);
        session.retrieveAndSetRemoteFlowControlAttributes(CAMQPSessionConstants.DEFAULT_OUTGOING_WINDOW_SIZE, 0, CAMQPSessionConstants.DEFAULT_INCOMING_WINDOW_SIZE);

        mockContext.checking(new Expectations()
            {{
                ignoring(mockStateActor).getCurrentState();will(returnValue(State.MAPPED));
            }}
        );
        return session;
    }

    public static ChannelBuffer createAttachFrame(long linkHandle)
    {
        CAMQPControlAttach attachControl = new CAMQPControlAttach();
        attachControl.setHandle(linkHandle);
        attachControl.setName("TestLink");
        attachControl.setMaxMessageSize(BigInteger.valueOf(16384L));

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlAttach.encode(encoder, attachControl);
        return encoder.getEncodedBuffer();
    }

    public static ChannelBuffer createDetachFrame(long linkHandle)
    {
        CAMQPControlDetach detachControl = new CAMQPControlDetach();
        detachControl.setHandle(linkHandle);
        detachControl.setClosed(true);

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlDetach.encode(encoder, detachControl);
        return encoder.getEncodedBuffer();
    }
}
