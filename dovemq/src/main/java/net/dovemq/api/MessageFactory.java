package net.dovemq.api;

import net.dovemq.transport.endpoint.DoveMQMessageImpl;

public class MessageFactory
{
    public static DoveMQMessage createMessage()
    {
        return new DoveMQMessageImpl();
    }
}
