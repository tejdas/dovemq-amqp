package net.dovemq.transport.endpoint;

import net.dovemq.transport.link.CAMQPLinkEndpoint;
import net.dovemq.transport.link.CAMQPLinkFactory;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.CAMQPLinkReceiver;
import net.dovemq.transport.link.CAMQPLinkSender;
import net.dovemq.transport.link.LinkRole;

public final class CAMQPEndpointManager
{
    private static CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();

    public static CAMQPEndpointPolicy getDefaultEndpointPolicy()
    {
        return defaultEndpointPolicy;
    }

    public static void setDefaultEndpointPolicy(CAMQPEndpointPolicy defaultEndpointPolicy)
    {
        CAMQPEndpointManager.defaultEndpointPolicy = defaultEndpointPolicy;
    }

    public static CAMQPSourceInterface createSource(String containerId, String source, String target)
    {
        return createSource(containerId, source, target, defaultEndpointPolicy);
    }

    public static CAMQPSourceInterface createSource(String containerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPLinkSender linkSender = CAMQPLinkFactory.createLinkSender(containerId, source, target, endpointPolicy);
        CAMQPSource dovemqSource = new CAMQPSource(linkSender, linkSender.getEndpointPolicy());
        linkSender.setSource(dovemqSource);
        return dovemqSource;
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
            CAMQPTarget dovemqTarget = new CAMQPTarget(linkReceiver, linkEndpoint.getEndpointPolicy());
            linkReceiver.setTarget(dovemqTarget);
            linkReceiver.configureSteadyStatePacedByMessageReceipt(10, 100);
            return dovemqTarget;
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");
        }
        return null;
    }

    public static CAMQPTargetInterface createTarget(String containerId, String source, String target)
    {
        return createTarget(containerId, source, target, defaultEndpointPolicy);
    }

    public static CAMQPTargetInterface createTarget(String containerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPLinkReceiver linkReceiver = CAMQPLinkFactory.createLinkReceiver(containerId, source, target, endpointPolicy);
        CAMQPTarget dovemqTarget = new CAMQPTarget(linkReceiver, linkReceiver.getEndpointPolicy());
        linkReceiver.setTarget(dovemqTarget);
        return dovemqTarget;
    }

    public static CAMQPSourceInterface attachSource(String linkSource, String linkTarget)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return null;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkSender)
        {
            CAMQPLinkSender linkSender = (CAMQPLinkSender) linkEndpoint;
            CAMQPSource dovemqSource = new CAMQPSource(linkSender, linkEndpoint.getEndpointPolicy());
            linkSender.setSource(dovemqSource);
            return dovemqSource;
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkSender");
        }
        return null;
    }
}
