package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public interface CAMQPLinkSenderInterface
{
    public void sendMessage(String deliveryTag, CAMQPMessagePayload message);
    public void messageSent(CAMQPControlTransfer transferFrame);
}
