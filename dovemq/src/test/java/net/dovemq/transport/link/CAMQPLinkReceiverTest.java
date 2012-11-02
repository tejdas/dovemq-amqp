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

import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.ReceiverLinkCreditPolicy;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
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

public class CAMQPLinkReceiverTest
{
    private static class MockLinkReceiverFactory implements CAMQPLinkMessageHandlerFactory
    {
        private CAMQPLinkReceiver linkReceiver = null;

        synchronized void setLinkReceiver(CAMQPLinkReceiver linkReceiver)
        {
            this.linkReceiver = linkReceiver;
        }
        @Override
        public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach)
        {
            synchronized (this)
            {
                return linkReceiver;
            }
        }
    }

    private class TestTarget implements CAMQPTargetInterface
    {
        public AtomicInteger numMessagesReceived = new AtomicInteger(0);
        @Override
        public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
        {
            numMessagesReceived.incrementAndGet();
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
        public void registerMessageReceiver(DoveMQMessageReceiver targetReceiver)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void acnowledgeMessageProcessingComplete()
        {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Sends messages on a CAMQPLinkSender.
     * If pauseBetweenSends is true, sleeps for 0-25 milliseconds
     * between each send.
     *
     */
    private class MessageSender extends BaseMessageSender
    {
        public MessageSender(int numMessagesToSend, boolean pauseBetweenSends, boolean checkIncomingFlow)
        {
            super(numMessagesToSend, pauseBetweenSends);
            this.checkIncomingFlow = checkIncomingFlow;
        }

        private final boolean checkIncomingFlow;
        public int linkCreditBoost;
        public int minLinkCreditThreshold;
        private int gotLinkCredit = 0;

        @Override
        public void run()
        {
            gotLinkCredit = linkCreditBoost;
            super.run();
        }

        @Override
        void doSend()
        {
            Random r = new Random();
            createAndSendMessage(r);

            gotLinkCredit--;
            if (checkIncomingFlow && (gotLinkCredit < minLinkCreditThreshold))
            {
                getAndAssertLinkCredit(linkCreditBoost);
                gotLinkCredit = linkCreditBoost;
            }
        }
    }

    private static final MockLinkReceiverFactory factory = new MockLinkReceiverFactory();
    private Mockery mockContext = null;
    private ExecutorService executor = null;
    private static final AtomicInteger numLinkFlowFrameCount = new AtomicInteger(0);

    private static final AtomicLong nextExpectedIncomingTransferId = new AtomicLong(0);

    private static final AtomicLong globalDeliveryId = new AtomicLong(0);

    private CAMQPSessionInterface session = null;
    private CAMQPIncomingChannelHandler frameHandler = null;

    private final BlockingQueue<ChannelBuffer> framesQueue = new LinkedBlockingQueue<ChannelBuffer>();;
    private final BlockingQueue<CAMQPControlTransfer> transferFramesQueue = new LinkedBlockingQueue<CAMQPControlTransfer>();
    private final BlockingQueue<Object> controlFramesQueue = new LinkedBlockingQueue<Object>();

    private CAMQPConnection mockConnection = null;

    public static CAMQPLinkReceiver linkReceiver = null;
    private TestTarget target = null;
    private long linkHandle = 1;
    private int sessionIncomingChannelId = 5;
    private Future<?> task = null;
    private FramesProcessor framesProcessor = null;

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
        linkReceiver = new CAMQPLinkReceiver(session);
        target = new TestTarget();
        linkReceiver.registerTarget(target);
        factory.setLinkReceiver(linkReceiver);
        attachHandshakeAndVerify(linkHandle);
    }

    @After
    public void tearDown()
    {
        mockContext.assertIsSatisfied();
        transferFramesQueue.clear();
        controlFramesQueue.clear();

        linkReceiver = null;
        nextExpectedIncomingTransferId.set(0);
        globalDeliveryId.set(0);
        task.cancel(true);
        executor.shutdown();
        framesProcessor = null;
    }

    @AfterClass
    public static void teardownAfterClass()
    {
        CAMQPSessionManager.shutdown();
    }

    /**
     * Sends one message on CAMQPLinkSender
     * @throws InterruptedException
     */
    @Test
    public void testSendMessageExceedLinkCredit() throws InterruptedException
    {
        linkReceiver.setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET);
        MessageSender sender = new MessageSender((int) CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT + 1, true, false);
        executor.submit(sender);

        Object control = null;
        try
        {
            control = controlFramesQueue.poll(6000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlDetach);
        CAMQPControlDetach detachControl = (CAMQPControlDetach) control;
        assertTrue(detachControl.isSetError());
        CAMQPDefinitionError error = detachControl.getError();
        assertTrue(error != null);
        assertEquals(error.getCondition(), CAMQPConstants.LINK_ERROR_TRANSFER_LIMIT_EXCEEDED);
        assertEquals(target.numMessagesReceived.get(), CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT);

        ChannelBuffer detachBuf = CAMQPSessionSenderTest.createDetachFrame(linkHandle);
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, detachBuf.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, detachBuf));

        sender.waitForDone();
    }

    @Test
    public void testProvideLinkCreditAndExceedLinkCredit() throws InterruptedException
    {
        int numMessages = 30;
        linkReceiver.getMessages(numMessages);

        getAndAssertLinkCredit(numMessages);

        MessageSender sender = new MessageSender((int) (numMessages + CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT + 1), true, false);
        executor.submit(sender);

        Object control = null;
        try
        {
            control = controlFramesQueue.poll(6000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlDetach);
        CAMQPControlDetach detachControl = (CAMQPControlDetach) control;
        assertTrue(detachControl.isSetError());
        CAMQPDefinitionError error = detachControl.getError();
        assertTrue(error != null);
        assertEquals(error.getCondition(), CAMQPConstants.LINK_ERROR_TRANSFER_LIMIT_EXCEEDED);
        assertEquals(target.numMessagesReceived.get(), numMessages + CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT);

        ChannelBuffer detachBuf = CAMQPSessionSenderTest.createDetachFrame(linkHandle);
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, detachBuf.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, detachBuf));

        sender.waitForDone();
    }

    @Test
    public void testFlowAndStop() throws InterruptedException
    {
        int numMessages = 50;
        int linkCreditBoost = 70;
        int minLinkCreditThreshold = 15;
        linkReceiver.configureSteadyStatePacedByMessageReceipt(minLinkCreditThreshold, linkCreditBoost);
        getAndAssertLinkCredit(linkCreditBoost);

        MessageSender sender = new MessageSender(numMessages, true, false);
        executor.submit(sender);

        while (true)
        {
            if (target.numMessagesReceived.get() ==  numMessages)
                break;

            Thread.sleep(1000);
        }

        sender.waitForDone();

        linkReceiver.stop();
        getAndAssertLinkCredit(0);
        detachHandshakeAndVerify(linkHandle);
    }

    @Test
    public void testMessageFlowSteadyState() throws InterruptedException
    {
        int numMessages = 2000;
        int linkCreditBoost = 40;
        int minLinkCreditThreshold = 15;
        linkReceiver.configureSteadyStatePacedByMessageReceipt(minLinkCreditThreshold, linkCreditBoost);

        getAndAssertLinkCredit(linkCreditBoost);

        MessageSender sender = new MessageSender(numMessages, false, true);
        sender.linkCreditBoost = linkCreditBoost;
        sender.minLinkCreditThreshold = minLinkCreditThreshold;
        executor.submit(sender);

        while (true)
        {
            if (target.numMessagesReceived.get() ==  numMessages)
                break;

            Thread.sleep(1000);
        }
        sender.waitForDone();
        detachHandshakeAndVerify(linkHandle);
    }

    @Test
    public void testProcessFlowMessageByLinkReceiverWithCreditDemandedBySender()
    {
        long availableMessages = 10L;
        linkReceiver.setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_AS_DEMANDED_BY_SENDER);

        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setEcho(true);
        flow.setAvailable(availableMessages);
        flow.setLinkCredit(0L);
        flow.setDeliveryCount(35L);
        linkReceiver.flowReceived(flow);

        getAndAssertLinkCredit(availableMessages);
    }

    @Test
    public void testProcessFlowMessageByLinkReceiverWithSteadyState()
    {
        long configuredCreditBoost = 10;
        long minCreditThreshold = 5;
        linkReceiver.configureSteadyStatePacedByMessageReceipt(minCreditThreshold, configuredCreditBoost);

        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setEcho(true);
        flow.setAvailable(10L);
        flow.setLinkCredit(0L);
        linkReceiver.flowReceived(flow);

        getAndAssertLinkCredit(configuredCreditBoost);
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

    private void createAndSendMessage(Random r)
    {
        String deliveryTag = UUID.randomUUID().toString();
        long deliveryId = globalDeliveryId.getAndIncrement();
        CAMQPControlTransfer transferFrame = new CAMQPControlTransfer();
        transferFrame.setDeliveryId(deliveryId);
        transferFrame.setMore(false);
        transferFrame.setHandle(linkHandle);
        transferFrame.setDeliveryTag(deliveryTag.getBytes());

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(encoder, transferFrame);
        int payloadSize = 256 * (r.nextInt(10) + 1);
        encoder.writePayload(new CAMQPMessagePayload(new byte[payloadSize]));
        ChannelBuffer messageBuf = encoder.getEncodedBuffer();
        CAMQPFrameHeader frameHeader = CAMQPFrameHeader.createFrameHeader(sessionIncomingChannelId, messageBuf.readableBytes());
        frameHandler.frameReceived(new CAMQPFrame(frameHeader, messageBuf));
    }

    private void getAndAssertLinkCredit(long expectedLinkCredit)
    {
        Object control = null;
        try
        {
            control = controlFramesQueue.poll(2000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            assertFalse(true);
        }
        assertTrue(control != null);
        assertTrue(control instanceof CAMQPControlFlow);
        CAMQPControlFlow flowFrame = (CAMQPControlFlow) control;
        assertTrue(flowFrame.isSetHandle());
        assertTrue(flowFrame.isSetLinkCredit());
        assertTrue(expectedLinkCredit == flowFrame.getLinkCredit());
    }
}
