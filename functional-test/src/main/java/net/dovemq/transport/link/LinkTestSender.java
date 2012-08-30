package net.dovemq.transport.link;

import java.io.IOException;

public class LinkTestSender
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        /*
         * Read args
         */
        String brokerIp = args[0];
        String publisherName = args[1];
        String source = args[2];
        String target = args[3];
          
        String brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);
        {
            CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(brokerContainerId, source, target);
            System.out.println("Sender Link created between : " + source + "  and: " + target);
            linkSender.destroyLink();
        }
        
        {
            CAMQPLinkReceiver linkReceiver = CAMQPLinkFactory.createLinkReceiver(brokerContainerId, source, target);
            System.out.println("Receiver Link created between : " + source + "  and: " + target);
            linkReceiver.destroyLink();
        }
        CAMQPLinkManager.shutdown();
    }
}
