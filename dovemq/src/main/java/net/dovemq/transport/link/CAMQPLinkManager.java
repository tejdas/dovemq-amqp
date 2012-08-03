package net.dovemq.transport.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

public class CAMQPLinkManager implements CAMQPLinkMessageHandlerFactory
{
    private static final AtomicLong nextLinkHandle = new AtomicLong(0);
    private static final CAMQPLinkManager linkManager = new CAMQPLinkManager();
    
    private static final Map<String, CAMQPLinkMessageHandler> outstandingLinks = new ConcurrentHashMap<String, CAMQPLinkMessageHandler>();
    
    static long getNextLinkHandle()
    {
        return nextLinkHandle.getAndIncrement();
    }
    
    public static void initialize()
    {
        CAMQPSessionManager.registerLinkReceiverFactory(linkManager);
    }

    @Override
    public CAMQPLinkMessageHandler createLinkReceiver(CAMQPSessionInterface session, CAMQPControlAttach attach)
    {
        /*
         * role denotes the role of the Peer that has sent the ATTACH frame
         */
        Boolean role = attach.getRole();
        String linkName = attach.getName();
        CAMQPLinkMessageHandler linkEndpoint = CAMQPLinkManager.unregisterOutstandingLink(linkName);
        if (linkEndpoint != null)
        {
            return linkEndpoint;
        }
        
        if (role == CAMQPLinkConstants.ROLE_RECEIVER)
        {
            return new CAMQPLinkSender(session);
        }
        else
        {
            return new CAMQPLinkReceiver(session);
        }
    }
    
    static void registerOutstandingLink(String linkName, CAMQPLinkMessageHandler linkEndpoint)
    {
        outstandingLinks.put(linkName, linkEndpoint);
    }
    
    private static CAMQPLinkMessageHandler unregisterOutstandingLink(String linkName)
    {
        return outstandingLinks.remove(linkName);
    }
}
