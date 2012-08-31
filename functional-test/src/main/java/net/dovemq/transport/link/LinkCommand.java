package net.dovemq.transport.link;

import java.util.Collection;

public class LinkCommand implements LinkCommandMBean
{
    private LinkTestTarget linkTargetEndpoint = new LinkTestTarget();
    
    @Override
    public void registerFactory(String factoryName)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void registerSource(String linkSource, String linkTarget)
    {
        // TODO Auto-generated method stub
        
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
            linkReceiver.issueLinkCredit(10);
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
        // TODO Auto-generated method stub
        return linkTargetEndpoint.getNumberOfMessagesReceived();
    }
}
