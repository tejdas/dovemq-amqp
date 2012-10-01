package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.CAMQPMessageDeliveryPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestSimple
{
    private static final String source = "src";
    private static final String target = "target";
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
        
        int messagesToSend = Integer.parseInt(args[3]);
        
        String messageDelvieryPolicyArg = args[4];
        System.out.println("Message Delivery Policy: " + messageDelvieryPolicyArg);
        
        CAMQPMessageDeliveryPolicy messageDelvieryPolicy = CAMQPMessageDeliveryPolicy.ExactlyOnce;
        if (messageDelvieryPolicyArg.equalsIgnoreCase("ExactlyOnce"))
            messageDelvieryPolicy = CAMQPMessageDeliveryPolicy.ExactlyOnce;
        else if (messageDelvieryPolicyArg.equalsIgnoreCase("AtleastOnce"))
            messageDelvieryPolicy = CAMQPMessageDeliveryPolicy.AtleastOnce;
        else if (messageDelvieryPolicyArg.equalsIgnoreCase("AtmostOnce"))
            messageDelvieryPolicy = CAMQPMessageDeliveryPolicy.AtmostOnce;
                
        brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        
        mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy(messageDelvieryPolicy);
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId, source, target, endpointPolicy);
        mbeanProxy.attachTarget(source,  target);
        
        Random randomGenerator = new Random();
        for (int i = 0; i < messagesToSend; i++)
        {
            CAMQPMessagePayload message = LinkTestUtils.createMessagePayload(randomGenerator);
            sender.sendMessage(message);
        }
        System.out.println("Done sending messages");
        
        while (true)
        {
            Thread.sleep(1000);
            long numMessagesReceivedAtRemote = mbeanProxy.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtRemote);
            if (numMessagesReceivedAtRemote == messagesToSend)
                break;
        }
 
        System.out.println("Done: sleeping for 5 seconds");
        Thread.sleep(5000);
        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
