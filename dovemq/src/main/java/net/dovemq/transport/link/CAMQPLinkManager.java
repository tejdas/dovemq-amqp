package net.dovemq.transport.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

/**
 * This class is used
 * @author tejdas
 *
 */
public final class CAMQPLinkManager implements CAMQPLinkMessageHandlerFactory
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

    /**
     * Called by Session layer upon the receipt of a Link attach frame.
     */
    @Override
    public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach)
    {
        /*
         * role denotes the role of the Peer that has sent the ATTACH frame
         */
        Boolean role = attach.getRole();
        String linkName = attach.getName();
        CAMQPLinkMessageHandler linkEndpoint = CAMQPLinkManager.unregisterOutstandingLink(linkName);
        if (linkEndpoint != null)
        {
            /*
             * We initiated the Link establishment handshake (via CAMQPLinkEndpoint.createLink())
             * The Link establishment is complete. Remove and return the previously registered
             * link endpoint.
             */
            return linkEndpoint;
        }
 
        /*
         * We are the link receptors. Create an appropriate link end-point,
         * based on the role of the peer.
         */
        if (role == CAMQPLinkConstants.ROLE_RECEIVER)
        {
            /*
             * Peer is a Link receiver.
             */
            return new CAMQPLinkSender(session);
        }
        else
        {
            /*
             * Peer is a Link sender.
             */
            return new CAMQPLinkReceiver(session);
        }
    }

    /**
     * If we are the initiators of the Link establishment handshake, we first register
     * the link end-point here. It will be removed in linkAccepted, upon completion of
     * the link establishment.
     * 
     * @param linkName
     * @param linkEndpoint
     */
    static void registerOutstandingLink(String linkName, CAMQPLinkMessageHandler linkEndpoint)
    {
        outstandingLinks.put(linkName, linkEndpoint);
    }
    
    private static CAMQPLinkMessageHandler unregisterOutstandingLink(String linkName)
    {
        return outstandingLinks.remove(linkName);
    }
}
