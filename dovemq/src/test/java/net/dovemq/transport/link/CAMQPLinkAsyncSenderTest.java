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

package net.dovemq.transport.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;
import net.dovemq.transport.session.CAMQPSessionSenderTest;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CAMQPLinkAsyncSenderTest
{
    private class TestSource implements CAMQPSourceInterface
    {
        private final Random r = new Random();
        public boolean moreMessageAvailableDynamically = false;
        public int numMoreMessages = 30;
        public AtomicInteger totalMessages = new AtomicInteger(0);

        @Override
        public CAMQPMessage getMessage()
        {
            if (totalMessages.get() == 0)
            {
                if (moreMessageAvailableDynamically)
                {
                    totalMessages.set(numMoreMessages);
                    moreMessageAvailableDynamically = false;
                }
                else
                    return null;
            }

            totalMessages.decrementAndGet();

            String deliveryTag = UUID.randomUUID().toString();
            CAMQPMessagePayload payload = createMessage(r);
            return new CAMQPMessage(deliveryTag, payload);
        }

        @Override
        public long getMessageCount()
        {
            return totalMessages.get();
        }

        @Override
        public void messageSent(long deliveryId, CAMQPMessage message)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds,
                boolean settleMode,
                Object newState)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void sendMessage(DoveMQMessage message)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void registerDispositionObserver(CAMQPMessageDispositionObserver observer)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public long getId()
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    private static class MockLinkReceiverFactory implements CAMQPLinkMessageHandlerFactory
    {
        private CAMQPLinkAsyncSender linkSender = null;

        synchronized void setLinkSender(CAMQPLinkAsyncSender linkSender)
        {
            this.linkSender = linkSender;
        }
        @Override
        public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach)
        {
            synchronized (this)
            {
                return linkSender;
            }
        }
    }

    private static final MockLinkReceiverFactory factory = new MockLinkReceiverFactory();
    private Mockery mockContext = null;
    private ExecutorService executor = null;
    private static final AtomicInteger numLinkFlowFrameCount = new AtomicInteger(0);

    private static final AtomicLong nextExpectedIncomingTransferId = new AtomicLong(0);

    private CAMQPSessionInterface session = null;
    private CAMQPIncomingChannelHandler frameHandler = null;

    private final BlockingQueue<ChannelBuffer> framesQueue = new LinkedBlockingQueue<ChannelBuffer>();;
    private final BlockingQueue<CAMQPControlTransfer> transferFramesQueue = new LinkedBlockingQueue<CAMQPControlTransfer>();
    private final BlockingQueue<Object> controlFramesQueue = new LinkedBlockingQueue<Object>();

    private CAMQPConnection mockConnection = null;

    public CAMQPLinkAsyncSender linkSender = null;
    private long linkHandle = 1;
    private int sessionIncomingChannelId = 5;
    private Future<?> task = null;
    private FramesProcessor framesProcessor = null;
    private TestSource testSource = null;

    @BeforeClass
    public static void setupBeforeClass()
    {
        CAMQPSessionManager.registerLinkReceiverFactory(factory);
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

        framesProcessor = new FramesProcessor(framesQueue, transferFramesQueue, controlFramesQueue);
        task = executor.submit(framesProcessor);
        numLinkFlowFrameCount.set(0);

        mockConnection =  createMockConnection();
        session = CAMQPSessionSenderTest.createMockSessionAndSetExpectations(mockContext, mockConnection);
        frameHandler = (CAMQPIncomingChannelHandler) session;

        linkHandle = 1;
        linkSender = new CAMQPLinkAsyncSender(session);
        testSource = new TestSource();
        linkSender.registerSource(testSource);
        factory.setLinkSender(linkSender);
        attachHandshakeAndVerify(linkHandle);
    }

    @After
    public void tearDown()
    {
        detachHandshakeAndVerify(linkHandle);
        mockContext.assertIsSatisfied();
        transferFramesQueue.clear();
        controlFramesQueue.clear();

        linkSender = null;
        nextExpectedIncomingTransferId.set(0);
        task.cancel(true);
        executor.shutdown();
        framesProcessor = null;
    }

    @AfterClass
    public static void teardownAfterClass()
    {
        CAMQPSessionManager.shutdown();
    }

    @Test
    public void testSendMessage() throws InterruptedException
    {
        int totalMessages = 100;
        testSource.totalMessages.set(totalMessages);
        simulateLinkFlowFrameReceipt(false, false, 100, 256);

        getAndAssertMessage(totalMessages);
        checkAndAssertDeliveryCount();
    }

    @Test
    public void testSendMessageMoreMessagesAvailable() throws InterruptedException
    {
        int totalMessages = 100;
        testSource.totalMessages.set(totalMessages);
        testSource.numMoreMessages = 50;
        testSource.moreMessageAvailableDynamically = true;
        simulateLinkFlowFrameReceipt(false, false, 200, 256);

        getAndAssertMessage(totalMessages + testSource.numMoreMessages);
        checkAndAssertDeliveryCount();
    }

    @Test
    public void testSendMessageAndDrain() throws InterruptedException
    {
        int totalMessages = 75;
        boolean drain = true;
        long linkCredit = 95;
        testSource.totalMessages.set(totalMessages);
        simulateLinkFlowFrameReceipt(drain, false, 95, 256);

        getAndAssertMessage(totalMessages);
        checkAndAssertDrained(linkCredit);
    }

    @Test
    public void testSendMessageExhaustSessionCreditAndGiveMoreCredit() throws InterruptedException
    {
        long totalMessages = 100;
        testSource.totalMessages.set((int) totalMessages);
        long initialSessionCredit = 20;
        simulateLinkFlowFrameReceipt(false, false, 50, initialSessionCredit);

        getAndAssertMessage((int) initialSessionCredit);

        Thread.sleep(2000);

        long updatedLinkCredit = 60;
        simulateLinkFlowFrameReceipt(false, false, updatedLinkCredit, 100);

        getAndAssertMessage((int) updatedLinkCredit);

        controlFramesQueue.clear();

        checkAndAssertDeliveryCount();
    }

    @Test
    public void testSendMessageExhaustLinkCreditAndGiveMoreCredit() throws InterruptedException
    {
        int totalMessages = 100;
        testSource.totalMessages.set(totalMessages);

        long linkCredit = 35;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, 256);

        getAndAssertMessage((int) linkCredit);

        Thread.sleep(2000);

        linkCredit = 60;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, 256);

        getAndAssertMessage((int) linkCredit);

        controlFramesQueue.clear();

        checkAndAssertDeliveryCount();
    }

    private void getAndAssertMessage(int expectedMessageCount) throws InterruptedException
    {
        int messageCount = 0;
        while (true)
        {
            CAMQPControlTransfer transferFrame = transferFramesQueue.poll(3500, TimeUnit.MILLISECONDS);
            if (transferFrame == null)
            {
                break;
            }

            messageCount++;
            nextExpectedIncomingTransferId.incrementAndGet();
        }
        assertEquals(expectedMessageCount, messageCount);
    }

    private CAMQPConnection createMockConnection()
    {
        return new CAMQPConnection() {
            @Override
            public void sendFrame(ChannelBuffer buffer, int channelId)
            {
                framesQueue.add(buffer);
            }
        };
    }

    private void simulateLinkFlowFrameReceipt(boolean drain, boolean echo, long linkCredit, long sessionCredit)
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);

        flow.setDrain(drain);
        flow.setEcho(echo);
        flow.setLinkCredit(linkCredit);
        flow.setIncomingWindow(sessionCredit);
        flow.setOutgoingWindow(256L);
        flow.setNextIncomingId(nextExpectedIncomingTransferId.get());
        flow.setDeliveryCount(nextExpectedIncomingTransferId.get());

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder, flow);
        ChannelBuffer frameBody = encoder.getEncodedBuffer();
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, frameBody.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, frameBody));
    }

    private void sendFlowFrame(boolean drain, boolean echo)
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);

        flow.setDrain(drain);
        flow.setEcho(echo);
        flow.setIncomingWindow(0L);
        flow.setNextIncomingId(nextExpectedIncomingTransferId.get());
        flow.setDeliveryCount(nextExpectedIncomingTransferId.get());

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder, flow);
        ChannelBuffer frameBody = encoder.getEncodedBuffer();
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, frameBody.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, frameBody));
    }

    private void attachHandshakeAndVerify(long linkHandle)
    {
        ChannelBuffer attachBuf = CAMQPSessionSenderTest.createAttachFrame(linkHandle);
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, attachBuf.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, attachBuf));

        Object control = null;
        try
        {
            control = controlFramesQueue.poll(3500, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlAttach);
    }

    private void detachHandshakeAndVerify(long linkHandle)
    {
        ChannelBuffer detachBuf = CAMQPSessionSenderTest.createDetachFrame(linkHandle);
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, detachBuf.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, detachBuf));

        Object control = null;
        try
        {
            control = controlFramesQueue.poll(3500, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlDetach);
    }

    private CAMQPMessagePayload createMessage(Random r)
    {
        int sectionSize = 256 * (r.nextInt(10) + 1);
        return new CAMQPMessagePayload(new byte[sectionSize]);
    }

    private void checkAndAssertDeliveryCount()
    {
        controlFramesQueue.clear();
        boolean echo = true;
        sendFlowFrame(false, echo);

        Object control = null;
        try
        {
            control = controlFramesQueue.poll(3500, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlFlow);

        CAMQPControlFlow flow = (CAMQPControlFlow) control;
        assertTrue("Assertion failed: expected: " + nextExpectedIncomingTransferId.get() + "   got: " + flow.getDeliveryCount(), nextExpectedIncomingTransferId.get() == flow.getDeliveryCount());
    }

    private void checkAndAssertDrained(long expectedDeliveryCount)
    {
        while (true)
        {
            Object control = null;
            try
            {
                control = controlFramesQueue.poll(3500, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                assertFalse(true);
            }
            if (control == null)
                break;
            assertTrue(control instanceof CAMQPControlFlow);

            CAMQPControlFlow flow = (CAMQPControlFlow) control;
            assertTrue("Assertion failed: expected: " + expectedDeliveryCount + "   got: " + flow.getDeliveryCount(), expectedDeliveryCount == flow.getDeliveryCount());
        }
    }
}
