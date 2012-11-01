/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.link;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;

public class LinkTestTargetReceiver implements DoveMQMessageReceiver, Runnable
{
    private volatile boolean shutdown = false;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final BlockingQueue<DoveMQMessage> msgQueue = new LinkedBlockingQueue<DoveMQMessage>();
    private volatile CAMQPSourceInterface source = null;
    private volatile Thread sender = null;

    @Override
    public void messageReceived(DoveMQMessage message)
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
                DoveMQMessage msg = msgQueue.poll(1000, TimeUnit.MILLISECONDS);
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
