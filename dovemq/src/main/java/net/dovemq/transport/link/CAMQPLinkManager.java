package net.dovemq.transport.link;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

enum LinkSenderType
{
    PUSH,
    PULL
}
/**
 * This class is used
 * @author tejdas
 *
 */
public final class CAMQPLinkManager implements CAMQPLinkMessageHandlerFactory
{
    private static final Logger log = Logger.getLogger(CAMQPLinkManager.class);

    private static final AtomicLong nextLinkHandle = new AtomicLong(0);
    private static final CAMQPLinkManager linkManager = new CAMQPLinkManager();
    private static CAMQPListener listener = null;
    
    public static CAMQPLinkManager getLinkmanager()
    {
        return linkManager;
    }

    private LinkSenderType linkSenderType = LinkSenderType.PULL;
    
    private final LinkHandshakeTracker linkHandshakeTracker = new LinkHandshakeTracker();
    
    private final ConcurrentMap<String, CAMQPLinkEndpoint> openLinks = new ConcurrentHashMap<String, CAMQPLinkEndpoint>();
    
    private final ConcurrentMap<CAMQPLinkKey, Set<String>> keyToLinkSets = new ConcurrentHashMap<CAMQPLinkKey, Set<String>>();

    static LinkHandshakeTracker getLinkHandshakeTracker()
    {
        return linkManager.linkHandshakeTracker;
    }
    
    static long getNextLinkHandle()
    {
        return nextLinkHandle.getAndIncrement();
    }
    
    public static void initialize(boolean isBroker, String containerId)
    {
        CAMQPConnectionManager.initialize(containerId);
        log.info("container ID: " + CAMQPConnectionManager.getContainerId());
        
        if (isBroker)
        {
            CAMQPConnectionManager.registerConnectionObserver(new CAMQPConnectionReaper());
            CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();
            listener = CAMQPListener.createCAMQPListener(defaultConnectionProps);
            listener.start();
        }
        CAMQPSessionManager.registerLinkReceiverFactory(linkManager);
    }
    
    public static void shutdown()
    {
        CAMQPSessionManager.shutdown();
        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        if (listener != null)
        {
            listener.shutdown();
        }
    }
    
    public CAMQPLinkEndpoint getLinkEndpoint(String source, String target)
    {
        CAMQPLinkKey linkKey = new CAMQPLinkKey(source, target);
      
        String linkName = null;
        synchronized(this)
        {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if ((linkSetByKey != null) && (!linkSetByKey.isEmpty()))
            {
                Iterator<String> iter = linkSetByKey.iterator();
                if (iter.hasNext())
                {
                    linkName = iter.next();
                }
            }
        }
        
        if (linkName != null)
        {
            return openLinks.get(linkName);
        }
        return null;
    }
    
    public CAMQPLinkEndpoint getLinkEndpoint(String linkName)
    {
        return openLinks.get(linkName);
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
        CAMQPLinkMessageHandler linkEndpoint = linkHandshakeTracker.unregisterOutstandingLink(linkName);
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
            if (linkSenderType == LinkSenderType.PUSH)
            {
                return new CAMQPLinkSender(session);
            }
            else
            {
                return new CAMQPLinkAsyncSender(session);
            }
        }
        else
        {
            /*
             * Peer is a Link sender.
             */
            return new CAMQPLinkReceiver(session);
        }
    }
    
    void registerLinkEndpoint(String linkName, CAMQPLinkKey linkKey, CAMQPLinkEndpoint linkEndpoint)
    {
        log.debug("registerLinkEndpoint: linkName: " + linkName + "  LinkKey: " + linkKey.toString());
        openLinks.put(linkName,  linkEndpoint);
        
        synchronized(this)
        {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if (linkSetByKey == null)
            {
                linkSetByKey = new LinkedHashSet<String>();
                keyToLinkSets.put(linkKey,  linkSetByKey);
            }
            linkSetByKey.add(linkName);
        }
    }
    
    void unregisterLinkEndpoint(String linkName, CAMQPLinkKey linkKey)
    {
        log.debug("unregisterLinkEndpoint: linkName: " + linkName + "  LinkKey: " + linkKey.toString());
        synchronized(this)
        {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if (linkSetByKey != null)
            {
                linkSetByKey.remove(linkName);
                if (linkSetByKey.isEmpty())
                {
                    keyToLinkSets.remove(linkKey);
                }
            }
        }
        openLinks.remove(linkName);
    }
}
