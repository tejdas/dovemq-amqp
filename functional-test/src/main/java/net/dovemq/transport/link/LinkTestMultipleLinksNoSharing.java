package net.dovemq.transport.link;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.session.SessionCommand;

public class LinkTestMultipleLinksNoSharing
{
    private static class LinkTestMessageSender extends CAMQPTestTask implements Runnable
    {
        private volatile CAMQPLinkSender linkSender = null;
        CAMQPLinkSender getLinkSender()
        {
            return linkSender;
        }

        private final String linkSource;
        private final String linkTarget;
        private final int numMessagesToSend;
        public LinkTestMessageSender(CountDownLatch startSignal,
                CountDownLatch doneSignal, String src, String target, int numMessagesToSend)
        {
            super(startSignal, doneSignal);
            this.linkSource = src;
            this.linkTarget = target;
            this.numMessagesToSend = numMessagesToSend;
        }

        @Override
        public void run()
        {
            CAMQPLinkSender sender = CAMQPLinkFactory.createLinkSender(brokerContainerId, linkSource, linkTarget);
            linkSender = sender;
            System.out.println("Sender Link created between : " + linkSource + "  and: " + linkTarget);
            
            String linkName = linkSender.getLinkName();
            
            mbeanProxy.registerTarget(linkSource, linkTarget);
            mbeanProxy.issueLinkCredit(linkName, 10);
            
            LinkTestUtils.sendMessagesOnLink(linkSender, numMessagesToSend);
            waitForReady();
            linkSender.destroyLink();
            done();
        }
    }
    
    private static String source;
    private static String target;
    private static String brokerContainerId ;
    private static LinkCommandMBean mbeanProxy;
    
    public static void main(String[] args) throws InterruptedException, IOException, MalformedObjectNameException
    {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);
        
        source = args[3];
        target = args[4];
          
        brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        
        SessionCommand localSessionCommand = new SessionCommand();
        localSessionCommand.sessionCreate(brokerContainerId);
        
        mbeanProxy = jmxWrapper.getLinkBean();
        
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);
        int numMessagesExpected = 500;
        
        LinkTestMessageSender[] senders = new LinkTestMessageSender[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            String src = String.format("%s%d", source, i);
            String targ = String.format("%s%d", target, i);
            LinkTestMessageSender sender = new LinkTestMessageSender(startSignal, doneSignal, src, targ, numMessagesExpected);
            senders[i] = sender;
            executor.submit(sender);
        }

        Random randomGenerator = new Random();
        int iterator = 0;
        while (true)
        {
            int randomInt = randomGenerator.nextInt(50);
            long messagesReceived = mbeanProxy.getNumMessagesReceived();
            System.out.println("got messages: " + messagesReceived + " issuing link credit: " + randomInt);
            if (messagesReceived == numMessagesExpected * numThreads)
            {
                break;
            }
            Thread.sleep(500);

            LinkTestMessageSender sender = senders[iterator % numThreads];
            iterator++;
            /*
             * Receiver-driven link-credit
             */
            mbeanProxy.issueLinkCredit(sender.getLinkSender().getLinkName(), randomInt);            
        }
        
        startSignal.countDown();
        
        doneSignal.await();
        Thread.sleep(2000);
        
        assertTrue(mbeanProxy.getNumMessagesReceived() == numMessagesExpected * numThreads);
        executor.shutdown();
        
        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
