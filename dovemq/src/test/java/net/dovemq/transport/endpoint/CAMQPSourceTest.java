package net.dovemq.transport.endpoint;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.dovemq.api.MessageFactory;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class CAMQPSourceTest extends TestCase {
    private static class TestLinkSender implements CAMQPLinkSenderInterface {
        final AtomicInteger messageCount = new AtomicInteger(0);
        @Override
        public void registerSource(CAMQPSourceInterface source) {
        }

        @Override
        public void sendMessage(CAMQPMessage message) {
            messageCount.incrementAndGet();
        }

        @Override
        public void messageSent(CAMQPControlTransfer transferFrame) {
        }

        @Override
        public long getHandle() {
            return 0;
        }

        @Override
        public CAMQPSessionInterface getSession() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSendMessageExpectRuntimeException() {

        long minThreshold = 18;
        long maxThreshold = 24;
        long maxWaitPeriodForUnsentDeliveryThreshold = 2000;
        final TestLinkSender linkSender = new TestLinkSender();
        CAMQPSource linkSource = new CAMQPSource(linkSender, maxThreshold, minThreshold, maxWaitPeriodForUnsentDeliveryThreshold);

        for (int i = 0; i < maxThreshold; i++) {
            linkSource.sendMessage(MessageFactory.createMessage());
        }
        long beginTime = System.currentTimeMillis();
        try {
            linkSource.sendMessage(MessageFactory.createMessage());
            assertFalse("Expected RuntimeException ", true);
        } catch (RuntimeException ex) {
            long endTime = System.currentTimeMillis();
            assertTrue(endTime-beginTime >= maxWaitPeriodForUnsentDeliveryThreshold);
            assertTrue(StringUtils.equals(CAMQPEndpointConstants.LINK_SENDER_CONGESTION_EXCEPTION, ex.getMessage()));
            assertEquals(maxThreshold, linkSender.messageCount.get());
        }
    }

    @Test
    public void testWaitUntilLinkDecongestionAndSendMessage() {

        final long minThreshold = 18;
        final long maxThreshold = 24;
        long maxWaitPeriodForUnsentDeliveryThreshold = 4000;
        final TestLinkSender linkSender = new TestLinkSender();
        final CAMQPSource linkSource = new CAMQPSource(linkSender, maxThreshold, minThreshold, maxWaitPeriodForUnsentDeliveryThreshold);

        for (int i = 0; i < maxThreshold; i++) {
            linkSource.sendMessage(MessageFactory.createMessage());
        }

        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch waitSignal = new CountDownLatch(1);
        final long runnableSleepTime = maxWaitPeriodForUnsentDeliveryThreshold/2;
        Runnable linkLayer = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();
                    waitSignal.countDown();
                    Thread.sleep(runnableSleepTime);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                for (int i = 0; i < minThreshold; i++) {
                    linkSource.messageSent(i, new CAMQPMessage(UUID.randomUUID().toString(), null));
                }
            }
        };

        new Thread(linkLayer).start();
        long beginTime = System.currentTimeMillis();
        startSignal.countDown();
        try {
            waitSignal.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            linkSource.sendMessage(MessageFactory.createMessage());
            long endTime = System.currentTimeMillis();
            assertTrue(endTime-beginTime >= runnableSleepTime);
            assertTrue(endTime-beginTime < maxWaitPeriodForUnsentDeliveryThreshold);
            assertEquals(maxThreshold+1, linkSender.messageCount.get());
        } catch (RuntimeException ex) {
            assertFalse("Did not expect RuntimeException ", true);
        }
    }
}
