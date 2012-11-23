package net.dovemq.api;

import net.dovemq.transport.endpoint.DoveMQMessageImpl;

/**
 * A factory to create DoveMQMessage
 * @author tejdas
 */
public final class MessageFactory
{
    public static DoveMQMessage createMessage()
    {
        return new DoveMQMessageImpl();
    }
}
