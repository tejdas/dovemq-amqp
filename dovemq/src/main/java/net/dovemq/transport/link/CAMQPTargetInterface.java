package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;

public interface CAMQPTargetInterface
{
    public void messageReceived(String deliveryTag, CAMQPMessagePayload message);
    public void messageStateChanged(String deliveryId, int oldState, int newState);
}
