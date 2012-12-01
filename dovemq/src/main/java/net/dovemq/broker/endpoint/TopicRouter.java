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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

import org.apache.commons.lang.StringUtils;

final class TopicRouter implements CAMQPMessageReceiver, CAMQPMessageDispositionObserver, Runnable
{
    private static class MessageContext
    {
        boolean hasExpired(long currentTime)
        {
            return (currentTime-contextCreationTime > 60000);
        }
        MessageContext(int numSubscribers, CAMQPTargetInterface sourceSink)
        {
            super();
            this.numSubscribers = new AtomicInteger(numSubscribers);
            this.sourceSink = sourceSink;
            contextCreationTime = System.currentTimeMillis();
        }
        final AtomicInteger numSubscribers;
        final long contextCreationTime;
        final CAMQPTargetInterface sourceSink;
    }

    private static class SubscriberContext
    {
        boolean canMessageBePublished(String messageTag)
        {
            if (StringUtils.isEmpty(messageTag))
            {
                return false;
            }

            Matcher matcher =  messageFilterPattern.matcher(messageTag);
            return matcher.matches();
        }

        CAMQPSourceInterface getSubscriberProxy()
        {
            return subscriberProxy;
        }

        boolean hasFilter()
        {
            return (messageFilterPattern != null);
        }

        public SubscriberContext(Pattern messageFilterPattern,
                CAMQPSourceInterface subscriberProxy)
        {
            super();
            this.messageFilterPattern = messageFilterPattern;
            this.subscriberProxy = subscriberProxy;
        }

        private final Pattern messageFilterPattern;
        private final CAMQPSourceInterface subscriberProxy;
    }

    private final Set<SubscriberContext> subscriberProxies = new CopyOnWriteArraySet<SubscriberContext>();
    private final Set<CAMQPTargetInterface> publisherSinks = new CopyOnWriteArraySet<CAMQPTargetInterface>();

    private final ConcurrentMap<Long, ConcurrentMap<Long, MessageContext>> inFlightMessagesByPublisherId =
            new ConcurrentHashMap<Long, ConcurrentMap<Long, MessageContext>>();

    /**
     * Acknowledge the completion of a message processing to a Publisher
     * only when all the subscribers have acked the message receipt.
     */
    @Override
    public void messageAckedByConsumer(DoveMQMessage message)
    {
        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        long sourceId = ((DoveMQMessageImpl) message).getSourceId();
        ConcurrentMap<Long, MessageContext> unackedMap = inFlightMessagesByPublisherId.get(sourceId);
        if (unackedMap != null)
        {
            MessageContext msgContext = unackedMap.get(deliveryId);
            if (msgContext != null)
            {
                int numSubscribersLeftToAck = msgContext.numSubscribers.decrementAndGet();
                if (0 == numSubscribersLeftToAck)
                {
                    if (null != unackedMap.remove(deliveryId))
                    {
                        msgContext.sourceSink.acknowledgeMessageProcessingComplete(deliveryId);
                    }
                }
            }
        }
    }

    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target)
    {
        long publisherId = target.getId();
        ((DoveMQMessageImpl) message).setSourceId(publisherId);

        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        ConcurrentMap<Long, MessageContext> inFlightMsgMap = inFlightMessagesByPublisherId.get(publisherId);
        inFlightMsgMap.put(deliveryId, new MessageContext(subscriberProxies.size(), target));

        String messageTag = message.getApplicationProperty(DoveMQMessageImpl.ROUTING_TAG_KEY);

        for (SubscriberContext subscriberProxy : subscriberProxies)
        {
            if (subscriberProxy.hasFilter())
            {
                if (subscriberProxy.canMessageBePublished(messageTag))
                {
                    subscriberProxy.getSubscriberProxy().sendMessage(message);
                }
            }
            else
            {
                subscriberProxy.getSubscriberProxy().sendMessage(message);
            }
        }
    }

    void subscriberAttached(CAMQPSourceInterface destination, CAMQPEndpointPolicy endpointPolicy)
    {
        Pattern messageFilterPattern = null;
        String messageFilterPatternString = endpointPolicy.getMessageFilterPattern();
        if (!StringUtils.isEmpty(messageFilterPatternString))
        {
            try
            {
                messageFilterPattern = Pattern.compile(messageFilterPatternString);
            }
            catch (PatternSyntaxException ex)
            {
                messageFilterPattern = null;
            }
        }
        subscriberProxies.add(new SubscriberContext(messageFilterPattern, destination));
        destination.registerDispositionObserver(this);
    }

    void subscriberDetached(CAMQPSourceInterface targetProxy)
    {
        for (SubscriberContext subscriberContext: subscriberProxies)
        {
            if (subscriberContext.subscriberProxy == targetProxy)
            {
                subscriberProxies.remove(subscriberContext);
            }
        }
    }

    void publisherAttached(CAMQPTargetInterface sourceSink)
    {
        publisherSinks.add(sourceSink);
        inFlightMessagesByPublisherId.put(sourceSink.getId(), new ConcurrentHashMap<Long, MessageContext>());
        sourceSink.registerMessageReceiver(this);
    }

    void publisherDetached(CAMQPTargetInterface source)
    {
        publisherSinks.remove(source);
        inFlightMessagesByPublisherId.remove(source.getId());
    }

    boolean isCompletelyDetached()
    {
        return (publisherSinks.isEmpty() && subscriberProxies.isEmpty());
    }

    @Override
    public void run()
    {
        long currentTime = System.currentTimeMillis();

        try
        {
            Set<Long> publisherIds = inFlightMessagesByPublisherId.keySet();
            for (long id : publisherIds)
            {
                ConcurrentMap<Long, MessageContext> inFlightMessages = inFlightMessagesByPublisherId.get(id);
                Set<Long> inflightMessageIDs = inFlightMessages.keySet();
                for (Long messageId : inflightMessageIDs)
                {
                    MessageContext messageContext = inFlightMessages.get(messageId);
                    if ((messageContext != null) && messageContext.hasExpired(currentTime))
                    {
                        if (null != inFlightMessages.remove(messageId))
                        {
                            messageContext.sourceSink.acknowledgeMessageProcessingComplete(messageId);
                        }
                    }
                }
            }
        }
        finally
        {
        }
    }
}
