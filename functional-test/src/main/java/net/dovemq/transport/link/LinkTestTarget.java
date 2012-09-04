package net.dovemq.transport.link;

import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestTarget implements CAMQPTargetInterface
{
    private final AtomicLong messageCount = new AtomicLong(0);
    
    @Override
    public void messageReceived(String deliveryTag, CAMQPMessagePayload message)
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
}
