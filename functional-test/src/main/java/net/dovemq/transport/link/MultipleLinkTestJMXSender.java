package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.session.SessionCommand;

public class MultipleLinkTestJMXSender
{
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
        
        SessionCommand localSessionCommand = new SessionCommand();
        localSessionCommand.sessionCreate(brokerContainerId);
        
        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();
        
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);
        int numMessagesExpected = 500;
        
        LinkTestMsgSender[] senders = new LinkTestMsgSender[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            String src = String.format("%s%d", source, i);
            String targ = String.format("%s%d", target, i);
            LinkTestMsgSender sender = new LinkTestMsgSender(startSignal, doneSignal, src, targ, brokerContainerId, numMessagesExpected, mbeanProxy);
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

            LinkTestMsgSender sender = senders[iterator % numThreads];
            iterator++;
            mbeanProxy.issueLinkCredit(sender.getLinkSender().getLinkName(), randomInt);            
        }
        
        startSignal.countDown();
        
        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();
        
        CAMQPLinkManager.shutdown();        
        jmxWrapper.cleanup();
    }
}
