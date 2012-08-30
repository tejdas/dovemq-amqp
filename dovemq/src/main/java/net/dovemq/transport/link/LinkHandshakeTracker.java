package net.dovemq.transport.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tejdas
 */
final class LinkHandshakeTracker
{
    private final Map<String, CAMQPLinkMessageHandler> outstandingLinks = new ConcurrentHashMap<String, CAMQPLinkMessageHandler>();
    

    /**
     * If we are the initiators of the Link establishment handshake, we first register
     * the link end-point here. It will be removed in linkAccepted, upon completion of
     * the link establishment.
     * 
     * @param linkName
     * @param linkEndpoint
     */
    void registerOutstandingLink(String linkName, CAMQPLinkMessageHandler linkEndpoint)
    {
        outstandingLinks.put(linkName, linkEndpoint);
    }
    
    CAMQPLinkMessageHandler unregisterOutstandingLink(String linkName)
    {
        return outstandingLinks.remove(linkName);
    }
}
