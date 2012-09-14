package net.dovemq.transport.link;

import java.util.Collection;

import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

public class LinkCommand implements LinkCommandMBean
{
    private volatile LinkTestTarget linkTargetEndpoint = new LinkTestTarget();
    private volatile LinkTestTargetReceiver linkTargetReceiver = null;
    private volatile CAMQPSourceInterface linkSource = null;
    private final LinkTestTargetReceiver linkTargetSharedReceiver = new LinkTestTargetReceiver();
    
    @Override
    public void registerFactory(String factoryName)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void registerSource(String linkSource, String linkTarget, long initialMessageCount)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkSender)
        {
            CAMQPLinkAsyncSender linkSender = (CAMQPLinkAsyncSender) linkEndpoint;
            linkSender.setSource(new LinkTestSource(initialMessageCount));
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkSender");           
        }
    }

    @Override
    public void registerTarget(String linkSource, String linkTarget)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiver linkReceiver = (CAMQPLinkReceiver) linkEndpoint;
            
            linkReceiver.setTarget(linkTargetEndpoint);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");           
        }
    }

    @Override
    public Collection<String> getLinks()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSenderLink(String source,
            String target,
            String remoteContainerId)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLinkCreditSteadyState(String linkName, long minLinkCreditThreshold, long linkCreditBoost)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkName);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiverInterface linkReceiver = (CAMQPLinkReceiverInterface) linkEndpoint;
            linkReceiver.flowMessages(minLinkCreditThreshold, linkCreditBoost);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");           
        }       
    }

    @Override
    public void issueLinkCredit(String linkName, long linkCreditBoost)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkName);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiverInterface linkReceiver = (CAMQPLinkReceiverInterface) linkEndpoint;
            linkReceiver.issueLinkCredit(linkCreditBoost);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");           
        }
    }

    @Override
    public long getNumMessagesReceived()
    {
        return linkTargetEndpoint.getNumberOfMessagesReceived();
    }

    @Override
    public void reset()
    {
        linkTargetEndpoint.resetNumberOfMessagesReceived();
        if (linkTargetReceiver != null)
            linkTargetReceiver.stop();
        linkSource = null;
        linkTargetReceiver = null;
        linkTargetSharedReceiver.stop();
    }

    @Override
    public void attachTarget(String source, String target)
    {
        linkTargetReceiver = new LinkTestTargetReceiver();
        CAMQPTargetInterface linkTarget = CAMQPEndpointManager.attachTarget(source, target);
        linkTarget.registerTargetReceiver(linkTargetReceiver);
    }

    @Override
    public long getNumMessagesReceivedAtTargetReceiver()
    {
        if (linkTargetReceiver != null)
            return linkTargetReceiver.getNumberOfMessagesReceived();
        else
            return linkTargetSharedReceiver.getNumberOfMessagesReceived();
    }

    @Override
    public void createSource(String source,
            String target,
            String remoteContainerId)
    {
        linkSource = CAMQPEndpointManager.createSource(remoteContainerId, source, target);
        if (linkTargetReceiver != null)
            linkTargetReceiver.setSource(linkSource);
    }

    @Override
    public void attachSharedTarget(String source, String target)
    {
        CAMQPTargetInterface linkTarget = CAMQPEndpointManager.attachTarget(source, target);
        linkTarget.registerTargetReceiver(linkTargetSharedReceiver);
    }
}
