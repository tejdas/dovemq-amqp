package net.dovemq.transport.link;

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
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;
import net.dovemq.transport.session.CAMQPSessionSenderTest;

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
        public CAMQPLinkMessageHandler createLinkReceiver(CAMQPSessionInterface session, CAMQPControlAttach attach)
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
        public void messageReceived(String deliveryTag, CAMQPMessagePayload payload)
        {
            numMessagesReceived.incrementAndGet();
        }

        @Override
        public void messageStateChanged(String deliveryId, int oldState, int newState)
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
            gotLinkCredit = linkCreditBoost + minLinkCreditThreshold;
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
        assertEquals(target.numMessagesReceived.get(), 5);
        
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
        
        MessageSender sender = new MessageSender((int) numMessages, true, false);
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