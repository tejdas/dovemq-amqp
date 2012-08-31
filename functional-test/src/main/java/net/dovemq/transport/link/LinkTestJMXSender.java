package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;
import net.dovemq.transport.common.JMXProxyWrapper;

public class LinkTestJMXSender
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
        
        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(brokerContainerId, source, target);
        System.out.println("Sender Link created between : " + source + "  and: " + target);
        
        String linkName = linkSender.getLinkName();
        
        mbeanProxy.registerTarget(source, target);
        mbeanProxy.issueLinkCredit(linkName, 10);
        
        Thread.sleep(2000);
 /*       
        String deliveryTag = "first";
        CAMQPMessagePayload payload = new CAMQPMessagePayload("Hello world".getBytes());
        
        linkSender.sendMessage(deliveryTag, payload);
*/
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);
        int numMessagesExpected = 100;
        
        for (int i = 0; i < numThreads; i++)
        {
            LinkTestMessageSender sender = new LinkTestMessageSender(startSignal, doneSignal, linkSender, numMessagesExpected);
            executor.submit(sender);
        }
        
        startSignal.countDown();

        Random randomGenerator = new Random();
        while (true)
        {
            int randomInt = randomGenerator.nextInt(50);
            long messagesReceived = mbeanProxy.getNumMessagesReceived();
            System.out.println("got messages: " + messagesReceived + " issuing link credit: " + randomInt);
            if (messagesReceived == numMessagesExpected * numThreads)
            {
                break;
            }
            Thread.sleep(1000);

            mbeanProxy.issueLinkCredit(linkName, randomInt);
            
        }
        
        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();
        
        linkSender.destroyLink();

        CAMQPLinkManager.shutdown();        
        
        jmxWrapper.cleanup();
    }
}
