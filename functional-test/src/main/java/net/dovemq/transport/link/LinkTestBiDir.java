package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestBiDir
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
          
        brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        
        String containerId = CAMQPConnectionManager.getContainerId();
        
        mbeanProxy = jmxWrapper.getLinkBean();
        
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId, source, target, new CAMQPEndpointPolicy());
        mbeanProxy.attachTarget(source,  target);
        
        mbeanProxy.createSource("reverseSrc", "reverseTar", containerId);
        LinkCommand localLinkCommand = new LinkCommand();
        localLinkCommand.attachTarget("reverseSrc", "reverseTar");
        
        
        Random randomGenerator = new Random();
        for (int i = 0; i < messagesToSend; i++)
        {
            CAMQPMessagePayload message = LinkTestUtils.createMessagePayload(randomGenerator);
            sender.sendMessage(message);
            //Thread.sleep(randomGenerator.nextInt(10) + 10);
        }
        System.out.println("Done sending messages");
        
        while (true)
        {
            Thread.sleep(1000);
            long numMessagesReceivedAtRemote = mbeanProxy.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtRemote);
            long numMessagesReceivedAtLocal = localLinkCommand.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtLocal);            
            if ((numMessagesReceivedAtRemote == messagesToSend) && (numMessagesReceivedAtLocal == messagesToSend))
                break;
        }
 
        System.out.println("Done: sleeping for 20 seconds");
        Thread.sleep(20000);
        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
