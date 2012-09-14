package net.dovemq.transport.endpoint;

import net.dovemq.transport.frame.CAMQPMessagePayload;

public interface CAMQPTargetReceiver
{
    public void messageReceived(CAMQPMessagePayload message);
}
