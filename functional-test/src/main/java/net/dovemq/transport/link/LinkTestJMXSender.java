package net.dovemq.transport.link;

import java.io.IOException;
import javax.management.MalformedObjectNameException;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.frame.CAMQPMessagePayload;

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
        mbeanProxy.setLinkCreditSteadyState(linkName, 5, 10);
        
        Thread.sleep(2000);
        
        String deliveryTag = "first";
        CAMQPMessagePayload payload = new CAMQPMessagePayload("Hello world".getBytes());
        
        linkSender.sendMessage(deliveryTag, payload);
        
        Thread.sleep(5000);
        linkSender.destroyLink();

        CAMQPLinkManager.shutdown();        
        
        jmxWrapper.cleanup();
    }
}
