package net.dovemq.transport.link;

import java.util.Collection;

public class LinkCommand implements LinkCommandMBean
{

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
            linkReceiver.setTarget(new LinkTestTarget());
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
}
