package net.dovemq.transport.link;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.common.JMXProxyWrapper;
import static org.junit.Assert.assertTrue;

public class LinkTestMultipleSendersSharingOneLink
{
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
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);
        
        String source = args[3];
        String target = args[4];
          
        String brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        
        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(brokerContainerId, source, target);
        linkSender.setMaxAvailableLimit(16348);
        System.out.println("Sender Link created between : " + source + "  and: " + target);
        
        String linkName = linkSender.getLinkName();
        
        mbeanProxy.registerTarget(source, target);
        /*
         * ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE
         */
        mbeanProxy.setLinkCreditSteadyState(linkName, 100, 500);
        
        Thread.sleep(2000);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);
        int numMessagesExpected = 1000;
        
        for (int i = 0; i < numThreads; i++)
        {
            LinkTestMessageSender sender = new LinkTestMessageSender(startSignal, doneSignal, linkSender, numMessagesExpected);
            executor.submit(sender);
        }
        
        startSignal.countDown();
        while (true)
        {
            long messagesReceived = mbeanProxy.getNumMessagesReceived();
            System.out.println("got messages: " + messagesReceived);
            if (messagesReceived == numMessagesExpected * numThreads)
            {
                break;
            }
            Thread.sleep(1000);
        }
        
        assertTrue(mbeanProxy.getNumMessagesReceived() == numMessagesExpected * numThreads);
        
        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();
        
        linkSender.destroyLink();

        CAMQPLinkManager.shutdown();        
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
