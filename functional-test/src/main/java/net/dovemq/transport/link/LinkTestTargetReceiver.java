package net.dovemq.transport.link;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetReceiver;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestTargetReceiver implements CAMQPTargetReceiver, Runnable
{
    private volatile boolean shutdown = false;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final BlockingQueue<CAMQPMessagePayload> msgQueue = new LinkedBlockingQueue<CAMQPMessagePayload>();
    private volatile CAMQPSourceInterface source = null;
    private volatile Thread sender = null;
    
    @Override
    public void messageReceived(CAMQPMessagePayload message)
    {
        long count = messageCount.incrementAndGet();
        if (count%10000 == 0)
            System.out.println("received messages: " + count);

        try
        {
            if (source != null)
                msgQueue.put(message);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public long getNumberOfMessagesReceived()
    {
        return messageCount.longValue();
    }
    
    @Override
    public void run()
    {
        while (!shutdown)
        {
            try
            {
                CAMQPMessagePayload msg = msgQueue.poll(1000, TimeUnit.MILLISECONDS);
                if (msg != null)
                    source.sendMessage(msg);
            }
            catch (InterruptedException e)
            {
                assertFalse(true);
            }
        }
    }
    
    void setSource(CAMQPSourceInterface source)
    {
        this.source = source;
        sender = new Thread(this);
        sender.start();
    }
    
    void stop()
    {
        messageCount.set(0);
        shutdown = true;
        if (sender != null)
        {
            try
            {
                sender.join(5000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
