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

package net.dovemq.broker.endpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

class TopicRouter implements CAMQPMessageReceiver, CAMQPMessageDispositionObserver
{
    private static class MessageContext
    {
        MessageContext(int numSubscribers, CAMQPTargetInterface sourceSink)
        {
            super();
            this.numSubscribers = new AtomicInteger(numSubscribers);
            this.sourceSink = sourceSink;
        }
        final AtomicInteger numSubscribers;
        final CAMQPTargetInterface sourceSink;
    }
    private final ConcurrentMap<Long, MessageContext> inFlightMessageQueue = new ConcurrentHashMap<Long, MessageContext>();

    private final Set<CAMQPSourceInterface> subscriberProxies = new CopyOnWriteArraySet<CAMQPSourceInterface>();
    private final Set<CAMQPTargetInterface> publisherSinks = new CopyOnWriteArraySet<CAMQPTargetInterface>();

    @Override
    public void messageAckedByConsumer(DoveMQMessage message)
    {
        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        MessageContext msgContext = inFlightMessageQueue.get(deliveryId);
        if (msgContext != null)
        {
            if (0 == msgContext.numSubscribers.decrementAndGet())
            {
                inFlightMessageQueue.remove(deliveryId);
                msgContext.sourceSink.acknowledgeMessageProcessingComplete(deliveryId);
            }
        }
    }

    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target)
    {
        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        inFlightMessageQueue.put(deliveryId, new MessageContext(subscriberProxies.size(), target));
        for (CAMQPSourceInterface subscriber : subscriberProxies)
        {
            subscriber.sendMessage(message);
        }
    }

    void subscriberAttached(CAMQPSourceInterface destination)
    {
        subscriberProxies.add(destination);
        destination.registerDispositionObserver(this);
    }

    void subscriberDetached(CAMQPSourceInterface targetProxy)
    {
        subscriberProxies.remove(targetProxy);
    }

    void publisherAttached(CAMQPTargetInterface sourceSink)
    {
        publisherSinks.add(sourceSink);
        sourceSink.registerMessageReceiver(this);
    }

    void publisherDetached(CAMQPTargetInterface source)
    {
        publisherSinks.remove(source);
    }

    boolean isCompletelyDetached()
    {
        return (publisherSinks.isEmpty() && subscriberProxies.isEmpty() && inFlightMessageQueue.isEmpty());
    }
}
