package net.dovemq.transport.endpoint;

import net.dovemq.transport.link.CAMQPLinkEndpoint;
import net.dovemq.transport.link.CAMQPLinkFactory;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.CAMQPLinkReceiver;
import net.dovemq.transport.link.CAMQPLinkSender;
import net.dovemq.transport.link.LinkRole;

public final class CAMQPEndpointManager
{
    public static CAMQPSourceInterface createSource(String containerId, String source, String target)
    {
        CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(containerId, source, target);
        linkSender.setMaxAvailableLimit(16348);
        CAMQPSource dovemqSource = new CAMQPSource(linkSender);
        linkSender.setSource(dovemqSource);
        return dovemqSource;
    }
    
    public static void createTarget()
    {
    }
    
    public static CAMQPTargetInterface attachTarget(String linkSource, String linkTarget)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return null;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiver linkReceiver = (CAMQPLinkReceiver) linkEndpoint;
            CAMQPTarget dovemqTarget = new CAMQPTarget(linkReceiver);
            linkReceiver.setTarget(dovemqTarget);
            linkReceiver.flowMessages(10, 100);
            return dovemqTarget;
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");           
        }
        return null;
    }
}
