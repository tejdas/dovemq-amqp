package net.dovemq.transport.link;

import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
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

public class CAMQPLinkSenderTest
{
    private static class MockLinkReceiverFactory implements CAMQPLinkMessageHandlerFactory
    {
        private CAMQPLinkSender linkSender = null;
        
        synchronized void setLinkSender(CAMQPLinkSender linkSender)
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

    /**
     * Sends messages on a CAMQPLinkSender.
     * If pauseBetweenSends is true, sleeps for 0-25 milliseconds
     * between each send.
     *
     */
    private static class MessageSender extends BaseMessageSender
    {
        public MessageSender(int numMessagesToSend, boolean pauseBetweenSends, CAMQPLinkSender linkSender)
        {
            super(numMessagesToSend, pauseBetweenSends);
            this.linkSender = linkSender;
        }

        private final CAMQPLinkSender linkSender;
        
        @Override
        void doSend()
        {
            Random r = new Random();
            String deliveryTag = UUID.randomUUID().toString();
            this.linkSender.sendMessage(deliveryTag, createMessage(r));
        } 
    }
    
    private static class MySession implements CAMQPSessionInterface
    {
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private final CountDownLatch latch;
        private final CountDownLatch latch2;
        private final AtomicLong deliveryId = new AtomicLong(0);
        public MySession()
        {
            super();
            this.latch = new CountDownLatch(1);
            this.latch2 = new CountDownLatch(1);
        }
        
        public void signalDoneTransfer()
        {
            latch.countDown();
        }
        
        public void waitSendTransfer()
        {
            try
            {
                latch2.await();
            }
            catch (InterruptedException e)
            {
            }
        }
        
        public void waitUntilAllMessagesSent()
        {
            synchronized (this)
            {
                while (messageCount.get() < 2)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        
        @Override
        public void sendLinkControlFrame(ChannelBuffer encodedLinkControlFrame)
        {        
        }

        @Override
        public void registerLinkReceiver(Long linkHandle, CAMQPLinkMessageHandler linkReceiver)
        {
        }

        @Override
        public long getNextDeliveryId()
        {
            return deliveryId.incrementAndGet();
        }

        @Override
        public void sendTransfer(CAMQPControlTransfer transfer, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender)
        {
            int count = messageCount.incrementAndGet();
            if (count == 1)
            {
                try
                {
                    latch2.countDown();
                    latch.await();
                }
                catch (InterruptedException e)
                {
                }
            }
            if (count == 2)
            {
                synchronized (this)
                {
                    notifyAll();
                }
            }
        }

        @Override
        public void sendFlow(CAMQPControlFlow flow)
        {
        }

        @Override
        public void ackTransfer(long transferId)
        {
        }

        @Override
        public void sendDisposition(long deliveryId, boolean settleMode, boolean role, Object newState)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void sendBatchedDisposition(Collection<Long> deliveryIds,
                boolean settleMode,
                boolean role,
                Object newState)
        {
            // TODO Auto-generated method stub
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
    
    public CAMQPLinkSender linkSender = null;
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
        linkSender = new CAMQPLinkSender(session);
        linkSender.setMaxAvailableLimit(4096);
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
    
    /**
     * Sends one message on CAMQPLinkSender
     * @throws InterruptedException
     */
    @Test
    public void testSendMessage() throws InterruptedException
    {   
        Random r = new Random();
        simulateLinkFlowFrameReceipt(false, false, 50, 256);
        
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(deliveryTag, createMessage(r));
        getAndAssertMessage(1);
        checkAndAssertDeliveryCount();
    }
    
    /**
     * Tests session-level flow control.
     * num > linkCredit > sessionCredit
     * 
     * @throws InterruptedException
     */
    @Test
    public void testSendMessageSessionFlowControl() throws InterruptedException
    {   
        int numMessagesToSend = 45;
        long linkCredit = 50;
        long sessionCredit = 20;
        Random r = new Random();
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        for (int i = 0; i < numMessagesToSend; i++)
        {
            String deliveryTag = UUID.randomUUID().toString();
            linkSender.sendMessage(deliveryTag, createMessage(r));
        }
        
        getAndAssertMessage((int) sessionCredit);
        
        Thread.sleep(2000);
        
        sessionCredit = 30;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        getAndAssertMessage(25);
        checkAndAssertDeliveryCount();
    }
    
    @Test
    public void testSendMessageLinkFlowControl() throws InterruptedException
    {   
        int numMessagesToSend = 45;
        long linkCredit = 20;
        long sessionCredit = 256;
        Random r = new Random();
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        for (int i = 0; i < numMessagesToSend; i++)
        {
            String deliveryTag = UUID.randomUUID().toString();
            linkSender.sendMessage(deliveryTag, createMessage(r));
        }
        
        getAndAssertMessage((int) linkCredit);
        
        Thread.sleep(2000);
        
        linkCredit = 30;
        boolean echo = true;
        simulateLinkFlowFrameReceipt(false, echo, linkCredit, sessionCredit);
        
        getAndAssertMessage(25);
        assertTrue(framesProcessor.linkFlowFrameCount.get() > 0);
        checkAndAssertDeliveryCount();
    }
    
    @Test
    public void testSendMessageMixFlowControl() throws InterruptedException
    {   
        int numMessagesToSend = 100;
        long linkCredit = 20;
        long sessionCredit = 40;
        Random r = new Random();
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        for (int i = 0; i < numMessagesToSend; i++)
        {
            String deliveryTag = UUID.randomUUID().toString();
            linkSender.sendMessage(deliveryTag, createMessage(r));
        }
        
        int messagesExpected = (int) linkCredit;
        
        getAndAssertMessage(messagesExpected);
        
        Thread.sleep(3000);
        
        linkCredit = 60;
        sessionCredit = 30;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        getAndAssertMessage(30);
        
        Thread.sleep(3000);
        
        linkCredit = 100;
        sessionCredit = 100;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
          
        getAndAssertMessage(50);
        checkAndAssertDeliveryCount();
    }
    
    @Test
    public void testSendMessageReduceLinkCredit() throws InterruptedException
    {   
        int numMessagesToSend = 2000;
                
        long linkCredit = 1000;
        long sessionCredit = 2024;
        
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        MessageSender sender = new MessageSender(numMessagesToSend, true, linkSender);
        executor.submit(sender);
        
        Thread.sleep(2000);
        
        linkCredit = 700;
        
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        getAndAssertMessage((int)linkCredit);
        checkAndAssertDeliveryCount();
        sender.waitForDone();
    }
    
    @Test
    public void testSendMessageSessionAndLinkFlowControlPauseBetweenSends() throws InterruptedException
    {   
        int numMessagesToSend = 3000;
        
        MessageSender sender = new MessageSender(numMessagesToSend, true, linkSender);
        executor.submit(sender);
        
        getAndAssertMessageSendFlowFrameIfNeeded(numMessagesToSend);
        assertTrue(framesProcessor.linkFlowFrameCount.get() > 0);
        checkAndAssertDeliveryCount();
        sender.waitForDone();
    }
    
    @Test
    public void testSendMessageSessionAndLinkFlowControl() throws InterruptedException
    {   
        int numMessagesToSend = 3000;
        
        linkSender.setMaxAvailableLimit(4096);
        MessageSender sender = new MessageSender(numMessagesToSend, false, linkSender);
        executor.submit(sender);
        
        getAndAssertMessageSendFlowFrameIfNeeded(numMessagesToSend);
        assertTrue(framesProcessor.linkFlowFrameCount.get() > 0);
        checkAndAssertDeliveryCount();
        sender.waitForDone();
    }
    
    @Test
    public void testSendMessageNoFlowControl() throws InterruptedException
    {   
        int numMessagesToSend = 3000;
        
        linkSender.setMaxAvailableLimit(4096);
        MessageSender sender = new MessageSender(numMessagesToSend, false, linkSender);
        executor.submit(sender);
        
        long linkCredit = 4096;
        long sessionCredit = 4096;
        
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
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
            if (messageCount == numMessagesToSend)
                break;
        }
        assertEquals(numMessagesToSend, messageCount);
        checkAndAssertDeliveryCount();
        sender.waitForDone();
    }
    
    @Test
    public void testDrain() throws InterruptedException
    {   
        int numMessagesToSend = 500;
                
        long linkCredit = 400;
        long sessionCredit = 2024;
        
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        MessageSender sender = new MessageSender(numMessagesToSend, true, linkSender);
        executor.submit(sender);
        
        getAndAssertMessage((int)linkCredit);
        
        Thread.sleep(50);
        
        int messagesLeftToReceive = numMessagesToSend - (int) linkCredit;
        int extraCredit = 300;
        
        linkCredit = messagesLeftToReceive + extraCredit;
        
        boolean drain = true;
        simulateLinkFlowFrameReceipt(drain, false, linkCredit, sessionCredit);
        
        getAndAssertMessage(100);
        checkAndAssertDrained(numMessagesToSend + extraCredit);
        sender.waitForDone();
    }
    
    @Test
    public void testDrain2() throws InterruptedException
    {   
        int numMessagesToSend = 1000;
                
        long linkCredit = 400;
        long sessionCredit = 2024;
        
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        MessageSender sender = new MessageSender(numMessagesToSend, true, linkSender);
        executor.submit(sender);
        
        getAndAssertMessage((int)linkCredit);
        
        Thread.sleep(50);
        
        long newlinkCredit = 250;
        
        boolean drain = true;
        simulateLinkFlowFrameReceipt(drain, false, newlinkCredit, sessionCredit);
        
        getAndAssertMessage(250);
        checkAndAssertDrained(linkCredit + newlinkCredit);
        sender.waitForDone();
    }
    
    @Test
    public void testParkMessageWhileSendInProgress() throws InterruptedException
    {
        final MySession localSession = new MySession();
        
        linkHandle = 1;
        final CAMQPLinkSender linkSender = new CAMQPLinkSender(localSession);
        linkSender.setMaxAvailableLimit(4096);
        factory.setLinkSender(linkSender);
        
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
 
        flow.setDrain(false);
        flow.setEcho(false);
        flow.setLinkCredit(20L);
        flow.setIncomingWindow(20L);
        flow.setNextIncomingId(nextExpectedIncomingTransferId.get());
        flow.setDeliveryCount(nextExpectedIncomingTransferId.get());
        linkSender.flowReceived(flow);
        
        final Random r = new Random();
        
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                String deliveryTag = UUID.randomUUID().toString();
                linkSender.sendMessage(deliveryTag, createMessage(r));
            }          
        };
        Thread t = new Thread(runnable);
        t.start();
        
        localSession.waitSendTransfer();
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(deliveryTag, createMessage(r));
        
        localSession.signalDoneTransfer();
        localSession.waitUntilAllMessagesSent();
    }
    
    @Test
    public void testFlowReceivedWithNoDeliveryCountSet()
    {   
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
 
        flow.setDrain(true);
        flow.setEcho(false);
        flow.setLinkCredit(20L);
        flow.setIncomingWindow(20L);
        flow.setNextIncomingId(nextExpectedIncomingTransferId.get());
        linkSender.flowReceived(flow);
        
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
    
    private void getAndAssertMessageSendFlowFrameIfNeeded(int expectedMessageCount) throws InterruptedException
    {
        Random r = new Random();
        
        long linkCredit = 75 + r.nextInt(25);
        long sessionCredit = linkCredit + 20;
        boolean sessionCreditMore = true;
        simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
        
        Date lastFrameReceivedTime = new Date();
        int messageCount = 0;
        int messageCountSinceLastFlow = 0;
        while (true)
        {
            long sleepTime = 1500 + r.nextInt(500);
            CAMQPControlTransfer transferFrame = transferFramesQueue.poll(sleepTime, TimeUnit.MILLISECONDS);
            if (transferFrame == null)
            {
                Date now = new Date();
                if ((now.getTime() - lastFrameReceivedTime.getTime()) > 3500L)
                {
                    break; // allow test to fail instead of hang
                }
                else
                {
                    /*
                     * Haven't got messages for almost 2 seconds.
                     * Send a flow frame giving more linkCredit and sessionCredit
                     * 
                     * Messages received since last flow-control should never exceed the session credit.
                     * But they might occasionally exceed the link credit if the session credit is more
                     * than the link credit and hence messages are throttled at the session layer.
                     * 
                     */
                    assertTrue(messageCountSinceLastFlow <= sessionCredit);
                    if ((linkCredit < sessionCredit) && (messageCount < expectedMessageCount))
                        assertTrue("assertion failed: linkCredit: " + linkCredit + " sessionCredit: " + sessionCredit + " messageCountSinceLastFlow: " + messageCountSinceLastFlow, messageCountSinceLastFlow >= linkCredit);
                    
                    messageCountSinceLastFlow = 0;
                    /*
                     * Alternate between giving more session credit vs giving more link credit.
                     * This would allow the CAMQPLinkSender to throttle at the link layer and
                     * at the session layer alternately.
                     */
                    if (sessionCreditMore)
                    {
                        linkCredit = 95 + r.nextInt(25);
                        sessionCredit = linkCredit - 20;;
                        sessionCreditMore = false;
                    }
                    else
                    {
                        linkCredit = 75 + r.nextInt(25);
                        sessionCredit = linkCredit + 20;
                        sessionCreditMore = true;
                    }
                    
                    simulateLinkFlowFrameReceipt(false, false, linkCredit, sessionCredit);
                    continue;
                }
            }
 
            lastFrameReceivedTime = new Date();
            messageCount++;
            messageCountSinceLastFlow++;
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
    
    private static CAMQPMessagePayload createMessage(Random r)
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
