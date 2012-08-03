package net.dovemq.transport.link;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.session.CAMQPSessionInterface;

public interface CAMQPLinkMessageHandlerFactory
{
    public CAMQPLinkMessageHandler createLinkReceiver(CAMQPSessionInterface session, CAMQPControlAttach attach);
}
