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
