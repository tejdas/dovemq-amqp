package net.dovemq.transport.link;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;
import net.dovemq.transport.common.JMXProxyWrapper;

public class LinkTestReceiver
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
        
        CAMQPLinkReceiver linkReceiver = CAMQPLinkFactory.createLinkReceiver(brokerContainerId, source, target);
        System.out.println("Receiver Link created between : " + source + "  and: " + target);
        long expectedMessageCount = 500;
        
        mbeanProxy.registerSource(source, target, expectedMessageCount);
        
        LinkTestTarget linktarget = new LinkTestTarget();
        linkReceiver.setTarget(linktarget);
        
        long requestedMessageCount = 0;
        
        Random randomGenerator = new Random();
        while (linktarget.getNumberOfMessagesReceived() < expectedMessageCount)
        {
            int randomInt = randomGenerator.nextInt(50) + 5;
            long messagesYetToBeReceived = expectedMessageCount - linktarget.getNumberOfMessagesReceived();
            
            if (randomInt > messagesYetToBeReceived)
            {
                requestedMessageCount = expectedMessageCount;
                System.out.println("Requesting extra number of messages: "  + (randomInt - messagesYetToBeReceived));
            }
            else
            {
                requestedMessageCount += randomInt;               
            }
            
            linkReceiver.getMessages(randomInt);
            
            while (linktarget.getNumberOfMessagesReceived() < requestedMessageCount)
                Thread.sleep(500);
            
            System.out.println("received " + requestedMessageCount + " messages so far");
        }
        
        assertTrue(linktarget.getNumberOfMessagesReceived() == expectedMessageCount);
        
        linkReceiver.destroyLink();

        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        
        jmxWrapper.cleanup();
    }
}
