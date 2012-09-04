package net.dovemq.transport.link;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

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
}
