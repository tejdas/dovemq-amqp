package net.dovemq.transport.endpoint;

import java.util.Collection;

import net.dovemq.transport.frame.CAMQPMessagePayload;

public interface CAMQPTargetInterface
{
    public void registerTargetReceiver(CAMQPTargetReceiver targetReceiver);
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode);
    public void messageStateChanged(String deliveryId, int oldState, int newState);
    
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState);
}
