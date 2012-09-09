package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkRoundTripTest
{
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
        
        mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId, source, target);
        
        mbeanProxy.attachTarget(source,  target);
        
        Random randomGenerator = new Random();
        for (int i = 0; i < 10; i++)
        {
            CAMQPMessagePayload message = LinkTestUtils.createMessagePayload(randomGenerator);
            sender.sendMessage(message);
        }
        System.out.println("Done sending messages");
        
        Thread.sleep(10000);

        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
