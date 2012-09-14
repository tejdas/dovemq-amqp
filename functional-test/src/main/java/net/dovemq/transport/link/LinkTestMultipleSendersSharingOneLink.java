package net.dovemq.transport.link;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.common.JMXProxyWrapper;

public class LinkTestMultipleSendersSharingOneLink
{
    private static final String source = "src";
    private static final String target = "target";
    private static int NUM_THREADS = 10;
    
    private static class LinkTestMessageSender extends CAMQPTestTask implements Runnable
    {
        private final CAMQPLinkSender linkSender;
        private final int numMessagesToSend;
        public LinkTestMessageSender(CountDownLatch startSignal,
                CountDownLatch doneSignal, CAMQPLinkSender linkSender, int numMessagesToSend)
        {
            super(startSignal, doneSignal);
            this.linkSender = linkSender;
            this.numMessagesToSend = numMessagesToSend;
        }

        @Override
        public void run()
        {
            waitForReady();
            LinkTestUtils.sendMessagesOnLink(linkSender, numMessagesToSend);
            done();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, MalformedObjectNameException
    {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];
        
        NUM_THREADS = Integer.parseInt(args[3]);
        int numMessagesToSend = Integer.parseInt(args[4]);
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);
        
        String brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        
        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(brokerContainerId, source, target);
        System.out.println("Sender Link created between : " + source + "  and: " + target);
        
        String linkName = linkSender.getLinkName();
        
        mbeanProxy.registerTarget(source, target);
        /*
         * ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE
         */
        mbeanProxy.setLinkCreditSteadyState(linkName, 100, 500);
        
        Thread.sleep(2000);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);
        
        for (int i = 0; i < NUM_THREADS; i++)
        {
            LinkTestMessageSender sender = new LinkTestMessageSender(startSignal, doneSignal, linkSender, numMessagesToSend);
            executor.submit(sender);
        }
        
        startSignal.countDown();
        while (true)
        {
            long messagesReceived = mbeanProxy.getNumMessagesReceived();
            System.out.println("got messages: " + messagesReceived);
            if (messagesReceived == numMessagesToSend * NUM_THREADS)
            {
                break;
            }
            Thread.sleep(1000);
        }
        
        assertTrue(mbeanProxy.getNumMessagesReceived() == numMessagesToSend * NUM_THREADS);
        
        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();
        
        linkSender.destroyLink();

        CAMQPLinkManager.shutdown();        
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}