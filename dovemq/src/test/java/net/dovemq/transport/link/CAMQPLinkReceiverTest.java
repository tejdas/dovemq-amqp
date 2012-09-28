package net.dovemq.transport.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
import static net.dovemq.transport.endpoint.CAMQPEndpointPolicy.ReceiverLinkCreditPolicy;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.CAMQPTargetReceiver;
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

    private static class TestTarget implements CAMQPTargetInterface
    {
        public AtomicInteger numMessagesReceived = new AtomicInteger(0);
        @Override
        public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
        {
            numMessagesReceived.incrementAndGet();
        }

        @Override
        public void messageStateChanged(String deliveryId, int oldState, int newState)
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
        public void registerTargetReceiver(CAMQPTargetReceiver targetReceiver)
        {
            // TODO Auto-generated method stub

        }
    }

    private static class TestDelayedTarget extends TestTarget
    {
        public TestDelayedTarget(CAMQPLinkReceiverInterface linkReceiver)
        {
            super();
            this.linkReceiver = linkReceiver;
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

        private static final Random r = new Random();

        private final ScheduledExecutorService _scheduledExecutor = Executors.newScheduledThreadPool(1);

        private final CAMQPLinkReceiverInterface linkReceiver;

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
        public void messageReceived(long deliveryId,
                String deliveryTag,
                CAMQPMessagePayload message,
                boolean settledBySender,
                int receiverSettleMode)
        {
            super.messageReceived(deliveryId,
                    deliveryTag,
                    message,
                    settledBySender,
                    receiverSettleMode);

            int msgProcessingTime = r.nextInt(300) + 50;
            messagesBeingProcessed.put(deliveryId,
                    new MsgDetails(System.currentTimeMillis(),
                            msgProcessingTime));
        }

        public void startProcessing()
        {
            _scheduledExecutor.scheduleWithFixedDelay(new MsgProcessor(),
                    300,
                    300,
                    TimeUnit.MILLISECONDS);
        }

        public void stopProcessing()
        {
            _scheduledExecutor.shutdown();
        }

        public boolean isDone()
        {
            return messagesBeingProcessed.isEmpty();
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

        private boolean configuredSteadyStatePacedByMessageProcessing = false;

        public void setConfiguredSteadyStatePacedByMessageProcessing(boolean configuredSteadyStatePacedByMessageProcessing)
        {
            this.configuredSteadyStatePacedByMessageProcessing = configuredSteadyStatePacedByMessageProcessing;
        }

        @Override
        public void run()
        {
            if (configuredSteadyStatePacedByMessageProcessing)
                gotLinkCredit = linkCreditBoost;
            else
                gotLinkCredit = linkCreditBoost + minLinkCreditThreshold;
            super.run();
        }

        @Override
        void doSend()
        {
            Random r = new Random();
            createAndSendMessage(r);

            gotLinkCredit--;
            if (configuredSteadyStatePacedByMessageProcessing && (gotLinkCredit < minLinkCreditThreshold))
            {
                long linkCredit = getLinkCreditWaitIfNecessary();
                System.out.println("got link credit: " + linkCredit);
                gotLinkCredit = (int) linkCredit;
            }
            else if (checkIncomingFlow && (gotLinkCredit < minLinkCreditThreshold))
            {
                getAndAssertLinkCredit(minLinkCreditThreshold+linkCreditBoost);
                gotLinkCredit = linkCreditBoost + minLinkCreditThreshold;
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
        linkReceiver.setTarget(target);
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
        linkReceiver.flowMessages(minLinkCreditThreshold, linkCreditBoost);
        getAndAssertLinkCredit(linkCreditBoost+minLinkCreditThreshold);

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
        linkReceiver.flowMessages(minLinkCreditThreshold, linkCreditBoost);

        getAndAssertLinkCredit(linkCreditBoost+minLinkCreditThreshold);

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
    public void testFoo() throws InterruptedException
    {
        int numMessages = 2000;
        int linkCreditBoost = 40;
        int minLinkCreditThreshold = 15;

        target = new TestDelayedTarget(linkReceiver);
        linkReceiver.setTarget(target);
        TestDelayedTarget delayedTarget = (TestDelayedTarget) target;
        delayedTarget.startProcessing();

        linkReceiver.configureSteadyStatePacedByMessageProcessing(minLinkCreditThreshold,
                linkCreditBoost);

        getAndAssertLinkCredit(linkCreditBoost);

        MessageSender sender = new MessageSender(numMessages, true, false);
        sender.setConfiguredSteadyStatePacedByMessageProcessing(true);
        sender.linkCreditBoost = linkCreditBoost;
        sender.minLinkCreditThreshold = minLinkCreditThreshold;
        executor.submit(sender);

        while (true)
        {
            if ((target.numMessagesReceived.get() == numMessages) && delayedTarget.isDone())
                break;

            Thread.sleep(1000);
        }
        System.out.println("done here");
        sender.waitForDone();
        delayedTarget.stopProcessing();
        controlFramesQueue.clear();
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
        linkReceiver.flowMessages(minCreditThreshold, configuredCreditBoost);

        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setEcho(true);
        flow.setAvailable(10L);
        flow.setLinkCredit(0L);
        linkReceiver.flowReceived(flow);

        getAndAssertLinkCredit(minCreditThreshold + configuredCreditBoost);
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
