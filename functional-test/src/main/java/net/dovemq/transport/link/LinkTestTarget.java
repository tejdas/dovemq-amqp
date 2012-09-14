package net.dovemq.transport.link;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.CAMQPTargetReceiver;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestTarget implements CAMQPTargetInterface
{
    private final AtomicLong messageCount = new AtomicLong(0);
    
    @Override
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
    {
        messageCount.incrementAndGet();
    }

    @Override
    public void messageStateChanged(String deliveryId,
            int oldState,
            int newState)
    {
    }
    
    public long getNumberOfMessagesReceived()
    {
        return messageCount.longValue();
    }
    
    public void resetNumberOfMessagesReceived()
    {
       messageCount.set(0);
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds,
            boolean settleMode,
            Object newState)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerTargetReceiver(CAMQPTargetReceiver targetReceiver)
    {
        // TODO Auto-generated method stub
        
    }
}
