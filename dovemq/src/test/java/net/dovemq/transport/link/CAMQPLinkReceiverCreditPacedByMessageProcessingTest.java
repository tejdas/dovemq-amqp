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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CAMQPLinkReceiverCreditPacedByMessageProcessingTest
{
    private boolean runWithSendFlow;
    private int numMessages;
    private int linkCreditBoost;
    private int minLinkCreditThreshold;
    private int msgProcessingAvgTime;

    public CAMQPLinkReceiverCreditPacedByMessageProcessingTest(boolean runWithSendFlow,
            int numMessages,
            int linkCreditBoost,
            int minLinkCreditThreshold,
            int msgProcessingAvgTime)
    {
        super();
        this.runWithSendFlow = runWithSendFlow;
        this.numMessages = numMessages;
        this.linkCreditBoost = linkCreditBoost;
        this.minLinkCreditThreshold = minLinkCreditThreshold;
        this.msgProcessingAvgTime = msgProcessingAvgTime;
    }

    @Parameters
    public static Collection<Object[]> configs()
    {
        return Arrays.asList(new Object[][]
            {
                {true, 2000, 400, 15, 200},
                {false, 2000, 400, 15, 200},
                {true, 1000, 45, 15, 20},
                {false, 1000, 45, 15, 20},
                {true, 1000, 45, 15, 2000},
                {false, 1000, 45, 15, 2000},
                {true, 2000, 400, 50, 3000},
                {false, 2000, 400, 50, 3000},
            }
        );
    }

    private static class MockLinkReceiverFactory implements
            CAMQPLinkMessageHandlerFactory
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

    private static class TestTarget implements CAMQPTargetInterface
    {
        private final int msgProcessingTime;
        public AtomicInteger numMessagesReceived = new AtomicInteger(0);

        public TestTarget(CAMQPLinkReceiverInterface linkReceiver,
                int msgProcessingTime)
        {
            super();
            this.linkReceiver = linkReceiver;
            this.msgProcessingTime = msgProcessingTime;
        }

        private static class MsgDetails
        {
            public MsgDetails(long receivedTime, int messageProcessingTime)
            {
                super();
                this.receivedTime = receivedTime;
                this.messageProcessingTime = messageProcessingTime;
            }

            public long receivedTime;

            public int messageProcessingTime;

            public boolean hasExpired(long newTime)
            {
                return (newTime - receivedTime > messageProcessingTime);
            }
        }

        private final ConcurrentMap<Long, MsgDetails> messagesBeingProcessed = new ConcurrentHashMap<Long, MsgDetails>();

        private final ScheduledExecutorService _scheduledExecutor = Executors.newScheduledThreadPool(1);

        private final CAMQPLinkReceiverInterface linkReceiver;

        private static final Random r = new Random();

        private class MsgProcessor implements Runnable
        {
            @Override
            public void run()
            {
                Set<Long> msgs = messagesBeingProcessed.keySet();
                long currentTime = System.currentTimeMillis();
                for (Long msg : msgs)
                {
                    MsgDetails msgDetail = messagesBeingProcessed.get(msg);
                    if ((msgDetail != null) && msgDetail.hasExpired(currentTime))
                    {
                        messagesBeingProcessed.remove(msg);
                        linkReceiver.acnowledgeMessageProcessingComplete();
                    }
                }
            }
        }

        @Override
        public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
        {
            numMessagesReceived.incrementAndGet();

            int msgProcessingTimeDelay = r.nextInt(msgProcessingTime) + 10;
            messagesBeingProcessed.put(deliveryId, new MsgDetails(System.currentTimeMillis(), msgProcessingTimeDelay));
        }

        public void startProcessing()
        {
            _scheduledExecutor.scheduleWithFixedDelay(new MsgProcessor(), 300, 300, TimeUnit.MILLISECONDS);
        }

        public void stopProcessing()
        {
            _scheduledExecutor.shutdown();
        }

        public boolean isDone()
        {
            return messagesBeingProcessed.isEmpty();
        }

        @Override
        public void registerMessageReceiver(DoveMQMessageReceiver targetReceiver)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void messageStateChanged(String deliveryId, int oldState, int newState)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void acnowledgeMessageProcessingComplete()
        {
        }
    }

    /**
     * Sends messages on a CAMQPLinkSender. If pauseBetweenSends is true, sleeps
     * for 0-25 milliseconds between each send.
     */
    private class MessageSender extends BaseMessageSender
    {
        public MessageSender(int numMessagesToSend, boolean pauseBetweenSends)
        {
            super(numMessagesToSend, pauseBetweenSends);
        }

        int linkCreditBoost;
        int minLinkCreditThreshold;
        volatile long gotLinkCredit = 0;

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

            if (gotLinkCredit < minLinkCreditThreshold)
            {
                long linkCredit = getLinkCreditWaitIfNecessary();
                gotLinkCredit = (int) linkCredit;
            }
        }
    }

    private class MessageAndFlowSender extends MessageSender
    {
        public MessageAndFlowSender(int numMessagesToSend, boolean pauseBetweenSends)
        {
            super(numMessagesToSend, pauseBetweenSends);
        }

        @Override
        void doSend()
        {
            CAMQPControlFlow control = peekForLinkCredit();
            if (control != null)
                gotLinkCredit = control.getLinkCredit();
            if (gotLinkCredit > 0)
            {
                Random r = new Random();
                createAndSendMessage(r);
                gotLinkCredit--;
            }

            while (gotLinkCredit <= 0)
            {
                CAMQPControlFlow flow = new CAMQPControlFlow();
                flow.setEcho(true);
                flow.setAvailable(10L);
                flow.setLinkCredit(gotLinkCredit);
                linkReceiver.flowReceived(flow);
                long linkCredit = getLinkCreditWaitIfNecessary();
                gotLinkCredit = linkCredit;

                if (gotLinkCredit <= 0)
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
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

        mockConnection = createMockConnection();
        session = CAMQPSessionSenderTest.createMockSessionAndSetExpectations(mockContext, mockConnection);
        frameHandler = (CAMQPIncomingChannelHandler) session;

        linkHandle = 1;
        linkReceiver = new CAMQPLinkReceiver(session);
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

    @Test
    public void testMessageProcessingLinkCredit() throws InterruptedException
    {
        target = new TestTarget(linkReceiver, msgProcessingAvgTime);
        linkReceiver.registerTarget(target);

        target.startProcessing();

        linkReceiver.configureSteadyStatePacedByMessageProcessing(minLinkCreditThreshold, linkCreditBoost);

        getAndAssertLinkCredit(linkCreditBoost);

        MessageSender sender;
        if (runWithSendFlow)
            sender = new MessageAndFlowSender(numMessages, true);
        else
            sender = new MessageSender(numMessages, true);
        sender.linkCreditBoost = linkCreditBoost;
        sender.minLinkCreditThreshold = minLinkCreditThreshold;
        executor.submit(sender);

        while (true)
        {
            if ((target.numMessagesReceived.get() == numMessages) && target.isDone())
                break;

            Thread.sleep(1000);
        }
        sender.waitForDone();
        target.stopProcessing();
        controlFramesQueue.clear();
        detachHandshakeAndVerify(linkHandle);
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
        long timeout = 2000L;
        long gotCredit = getLinkCredit(timeout);
        assertTrue(expectedLinkCredit == gotCredit);
    }

    private long getLinkCredit(long timeout)
    {
        Object control = null;
        try
        {
            control = controlFramesQueue.poll(timeout, TimeUnit.MILLISECONDS);
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
        return flowFrame.getLinkCredit();
    }

    private CAMQPControlFlow peekForLinkCredit()
    {
        Object control = controlFramesQueue.poll();
        if (control != null)
        {
            assertTrue(control instanceof CAMQPControlFlow);
            CAMQPControlFlow flowFrame = (CAMQPControlFlow) control;
            assertTrue(flowFrame.isSetHandle());
            assertTrue(flowFrame.isSetLinkCredit());
            return flowFrame;
        }
        return null;
    }

    private long getLinkCreditWaitIfNecessary()
    {
        Object control = null;
        while (true)
        {
            try
            {
                control = controlFramesQueue.poll(5000L, TimeUnit.MILLISECONDS);
                if (control != null)
                    break;
            }
            catch (InterruptedException e)
            {
                assertFalse(true);
            }
        }
        assertTrue(control instanceof CAMQPControlFlow);
        CAMQPControlFlow flowFrame = (CAMQPControlFlow) control;
        assertTrue(flowFrame.isSetHandle());
        assertTrue(flowFrame.isSetLinkCredit());
        return flowFrame.getLinkCredit();
    }
}
