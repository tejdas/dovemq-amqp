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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

import org.apache.commons.lang.StringUtils;

final class TopicRouter implements
        CAMQPMessageReceiver,
        CAMQPMessageDispositionObserver,
        Runnable {
    private static class MessageContext {
        boolean hasExpired(long currentTime) {
            return (currentTime - contextCreationTime > 60000);
        }

        MessageContext(int numSubscribers, CAMQPTargetInterface sourceSink) {
            super();
            this.numSubscribers = new AtomicInteger(numSubscribers);
            this.sourceSink = sourceSink;
            contextCreationTime = System.currentTimeMillis();
        }

        final AtomicInteger numSubscribers;

        final long contextCreationTime;

        final CAMQPTargetInterface sourceSink;
    }

    private final String topicName;

    private final TopicRouterType routerType;

    private final Set<RoutingEvaluator> subscriberProxies = new CopyOnWriteArraySet<RoutingEvaluator>();

    private final Set<CAMQPTargetInterface> publisherSinks = new CopyOnWriteArraySet<CAMQPTargetInterface>();

    private final ConcurrentMap<Long, ConcurrentMap<Long, MessageContext>> inFlightMessagesByPublisherId = new ConcurrentHashMap<Long, ConcurrentMap<Long, MessageContext>>();

    TopicRouter(String topicName, TopicRouterType routerType) {
        super();
        this.topicName = topicName;
        this.routerType = routerType;
    }

    /**
     * Acknowledge the completion of a message processing to a Publisher only
     * when all the subscribers have acked the message receipt.
     */
    @Override
    public void messageAckedByConsumer(DoveMQMessage message, CAMQPSourceInterface source) {
        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        long sourceId = ((DoveMQMessageImpl) message).getSourceId();
        ConcurrentMap<Long, MessageContext> unackedMap = inFlightMessagesByPublisherId.get(sourceId);
        if (unackedMap != null) {
            MessageContext msgContext = unackedMap.get(deliveryId);
            if (msgContext != null) {
                int numSubscribersLeftToAck = msgContext.numSubscribers.decrementAndGet();
                if (0 == numSubscribersLeftToAck) {
                    if (null != unackedMap.remove(deliveryId)) {
                        msgContext.sourceSink.acknowledgeMessageProcessingComplete(deliveryId);
                    }
                }
            }
        }
    }

    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target) {
        long publisherId = target.getId();
        ((DoveMQMessageImpl) message).setSourceId(publisherId);

        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        ConcurrentMap<Long, MessageContext> inFlightMsgMap = inFlightMessagesByPublisherId.get(publisherId);
        inFlightMsgMap.put(deliveryId, new MessageContext(subscriberProxies.size(), target));

        Object routingEvaluationContext = null;
        if (routerType == TopicRouterType.MessageTagFilter) {
            routingEvaluationContext = message.getApplicationProperty(DoveMQMessageImpl.ROUTING_TAG_KEY);
        }
        else if (routerType == TopicRouterType.Hierarchical) {
            routingEvaluationContext = message.getApplicationProperty(DoveMQMessageImpl.TOPIC_PUBLISH_HIERARCHY_KEY);
        }

        for (RoutingEvaluator subscriberProxy : subscriberProxies) {
            if (subscriberProxy.canMessageBePublished(routingEvaluationContext)) {
                subscriberProxy.getSubscriberProxy().sendMessage(message);
            }
        }
    }

    void subscriberAttached(CAMQPSourceInterface destination, CAMQPEndpointPolicy endpointPolicy) {
        if (routerType == TopicRouterType.MessageTagFilter) {
            Pattern messageFilterPattern = null;
            String messageFilterPatternString = endpointPolicy.getMessageFilterPattern();
            if (!StringUtils.isEmpty(messageFilterPatternString)) {
                try {
                    messageFilterPattern = Pattern.compile(messageFilterPatternString);
                }
                catch (PatternSyntaxException ex) {
                    messageFilterPattern = null;
                }
            }
            subscriberProxies.add(new RoutingMessageTagFilterEvaluator(messageFilterPattern, destination));
        }
        else if (routerType == TopicRouterType.Hierarchical) {
            String subscriptionTopicHierarchy = endpointPolicy.getSubscriptionTopicHierarchy();
            if (!StringUtils.isEmpty(subscriptionTopicHierarchy)) {
                subscriberProxies.add(new RoutingTopicHeirarchyMatcher(subscriptionTopicHierarchy, destination));
            }
            else {
                subscriberProxies.add(new RoutingTopicHeirarchyMatcher(topicName, destination));
            }

        }
        else {
            subscriberProxies.add(new RoutingEvaluator(destination));
        }

        destination.registerDispositionObserver(this);
    }

    void subscriberDetached(CAMQPSourceInterface targetProxy) {
        for (RoutingEvaluator subscriberContext : subscriberProxies) {
            if (subscriberContext.getSubscriberProxy() == targetProxy) {
                subscriberProxies.remove(subscriberContext);
            }
        }
    }

    void publisherAttached(CAMQPTargetInterface sourceSink) {
        publisherSinks.add(sourceSink);
        inFlightMessagesByPublisherId.put(sourceSink.getId(), new ConcurrentHashMap<Long, MessageContext>());
        sourceSink.registerMessageReceiver(this);
    }

    void publisherDetached(CAMQPTargetInterface source) {
        publisherSinks.remove(source);
        inFlightMessagesByPublisherId.remove(source.getId());
    }

    boolean isCompletelyDetached() {
        return (publisherSinks.isEmpty() && subscriberProxies.isEmpty());
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        try {
            Set<Long> publisherIds = inFlightMessagesByPublisherId.keySet();
            for (long id : publisherIds) {
                ConcurrentMap<Long, MessageContext> inFlightMessages = inFlightMessagesByPublisherId.get(id);
                Set<Long> inflightMessageIDs = inFlightMessages.keySet();
                for (Long messageId : inflightMessageIDs) {
                    MessageContext messageContext = inFlightMessages.get(messageId);
                    if ((messageContext != null) && messageContext.hasExpired(currentTime)) {
                        if (null != inFlightMessages.remove(messageId)) {
                            messageContext.sourceSink.acknowledgeMessageProcessingComplete(messageId);
                        }
                    }
                }
            }
        }
        finally {
        }
    }

    String getTopicName() {
        return topicName;
    }

    TopicRouterType getRouterType() {
        return routerType;
    }
}
