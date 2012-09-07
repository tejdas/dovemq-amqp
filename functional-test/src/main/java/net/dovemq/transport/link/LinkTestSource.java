package net.dovemq.transport.link;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestSource implements CAMQPSourceInterface
{
    private final AtomicLong messageCount;
    public LinkTestSource(long initialMessageCount)
    {
        messageCount = new AtomicLong(initialMessageCount);
    }
    private final Random randomGenerator = new Random();
    @Override
    public CAMQPMessage getMessage()
    {
        if (messageCount.getAndDecrement() > 0)
            return LinkTestUtils.createMessage(randomGenerator);
        else
            return null;
    }

    @Override
    public long getMessageCount()
    {
        return messageCount.get();
    }

    @Override
    public void messageStateChanged(String deliveryId, int oldState, int newState)
    {
    }

    @Override
    public void messageSent(long deliveryId, CAMQPMessage message)
    {
        // TODO Auto-generated method stub
        
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
    public void sendMessage(CAMQPMessagePayload message)
    {
        // TODO Auto-generated method stub
        
    }
}
