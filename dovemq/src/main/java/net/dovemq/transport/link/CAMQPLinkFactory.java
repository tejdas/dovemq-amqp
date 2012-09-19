package net.dovemq.transport.link;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionFactory;

/**
 * Factory class to initiate creation of Link Sender and Link Receiver
 * @author tejdas
 */
public final class CAMQPLinkFactory
{
    public static CAMQPLinkSender createLinkSender(String targetContainerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPSessionInterface session = CAMQPSessionFactory.getOrCreateCAMQPSession(targetContainerId);
        if (session != null)
        {
            CAMQPLinkSender sender = new CAMQPLinkSender(session);
            sender.createLink(source, target, endpointPolicy);
            return sender;
        }
        return null;
    }
    
    public static CAMQPLinkReceiver createLinkReceiver(String targetContainerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPSessionInterface session = CAMQPSessionFactory.getOrCreateCAMQPSession(targetContainerId);
        if (session != null)
        {
            CAMQPLinkReceiver receiver = new CAMQPLinkReceiver(session);
            receiver.createLink(source, target, endpointPolicy);
            return receiver;
        }
        return null;
    }

    public void linkReceiverCreated()
    {
    }
}
