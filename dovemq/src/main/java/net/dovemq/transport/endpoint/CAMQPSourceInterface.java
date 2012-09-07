package net.dovemq.transport.endpoint;

import java.util.Collection;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPMessage;

public interface CAMQPSourceInterface
{
    public void sendMessage(CAMQPMessagePayload message);
    public CAMQPMessage getMessage();
    public long getMessageCount();
    public void messageStateChanged(String deliveryId, int oldState, int newState);
    
    public void messageSent(long deliveryId, CAMQPMessage message);
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean settleMode, Object newState);
}
