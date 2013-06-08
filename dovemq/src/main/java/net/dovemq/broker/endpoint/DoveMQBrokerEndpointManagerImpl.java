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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.EndpointType;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

import org.apache.log4j.Logger;

public final class DoveMQBrokerEndpointManagerImpl extends DoveMQAbstractEndpointManager {
    private static final Logger log = Logger.getLogger(DoveMQBrokerEndpointManagerImpl.class);

    private final ConcurrentMap<String, QueueRouter> queueRouters = new ConcurrentHashMap<>();

    private final Map<TopicRouterType, ConcurrentMap<String, TopicRouter>> topicRoutersMap = new HashMap<>();

    public DoveMQBrokerEndpointManagerImpl() {
        super();
        topicRoutersMap.put(TopicRouterType.Basic, new ConcurrentHashMap<String, TopicRouter>());
        topicRoutersMap.put(TopicRouterType.MessageTagFilter, new ConcurrentHashMap<String, TopicRouter>());
        topicRoutersMap.put(TopicRouterType.Hierarchical, new ConcurrentHashMap<String, TopicRouter>());
    }

    @Override
    public void sourceEndpointAttached(String endpointName, CAMQPSourceInterface sourceEndpoint, CAMQPEndpointPolicy endpointPolicy) {
        if (endpointPolicy.getEndpointType() == EndpointType.QUEUE) {
            consumerAttached(endpointName, sourceEndpoint, endpointPolicy);
        }
        else {
            subscriberAttached(endpointName, sourceEndpoint, endpointPolicy);
        }
    }

    @Override
    public void sourceEndpointDetached(String endpointName, CAMQPSourceInterface sourceEndpoint, CAMQPEndpointPolicy endpointPolicy) {
        if (endpointPolicy.getEndpointType() == EndpointType.QUEUE) {
            consumerDetached(endpointName, sourceEndpoint);
        }
        else {
            subscriberDetached(endpointName, sourceEndpoint, endpointPolicy);
        }
    }

    @Override
    public void targetEndpointAttached(String endpointName, CAMQPTargetInterface targetEndpoint, CAMQPEndpointPolicy endpointPolicy) {
        if (endpointPolicy.getEndpointType() == EndpointType.QUEUE) {
            producerAttached(endpointName, targetEndpoint, endpointPolicy);
        }
        else {
            publisherAttached(endpointName, targetEndpoint, endpointPolicy);
        }
    }

    @Override
    public void targetEndpointDetached(String endpointName, CAMQPTargetInterface targetEndpoint, CAMQPEndpointPolicy endpointPolicy) {
        if (endpointPolicy.getEndpointType() == EndpointType.QUEUE) {
            producerDetached(endpointName, targetEndpoint);
        }
        else {
            publisherDetached(endpointName, targetEndpoint, endpointPolicy);
        }
    }

    void producerAttached(String queueName, CAMQPTargetInterface producer, CAMQPEndpointPolicy endpointPolicy) {
        QueueRouter queueRouter = new QueueRouter(queueName);
        QueueRouter queueRouterInMap = queueRouters.putIfAbsent(queueName, queueRouter);
        if (queueRouterInMap == null) {
            log.debug("creating queue: " + queueName);
            queueRouter.producerAttached(producer);
        }
        else {
            queueRouterInMap.producerAttached(producer);
        }
    }

    void producerDetached(String queueName, CAMQPTargetInterface producer) {
        QueueRouter queueRouter = queueRouters.get(queueName);
        if (queueRouter != null) {
            queueRouter.producerDetached(producer);
            if (queueRouter.isCompletelyDetached()) {
                log.debug("Removing queue: " + queueName);
                queueRouters.remove(queueName);
            }
        }
        log.debug("Producer detached from queue: " + queueName);
    }

    void consumerAttached(String queueName, CAMQPSourceInterface consumer, CAMQPEndpointPolicy endpointPolicy) {
        QueueRouter queueRouter = new QueueRouter(queueName);
        QueueRouter queueRouterInMap = queueRouters.putIfAbsent(queueName, queueRouter);
        if (queueRouterInMap == null) {
            queueRouter.consumerAttached(consumer);
        }
        else {
            queueRouterInMap.consumerAttached(consumer);
        }
    }

    void consumerDetached(String queueName, CAMQPSourceInterface consumer) {
        QueueRouter queueRouter = queueRouters.get(queueName);
        if (queueRouter != null) {
            queueRouter.consumerDetached(consumer);
            if (queueRouter.isCompletelyDetached()) {
                log.debug("Removing queue: " + queueName);
                queueRouters.remove(queueName);
            }
        }
        log.debug("Consumer detached from queue: " + queueName);
    }

    void publisherAttached(String topicName, CAMQPTargetInterface publisher, CAMQPEndpointPolicy endpointPolicy) {
        TopicRouterType routerType = endpointPolicy.getTopicRouterType();
        TopicRouter topicRouter = new TopicRouter(topicName, routerType);

        ConcurrentMap<String, TopicRouter> topicRouters = topicRoutersMap.get(routerType);
        TopicRouter topicRouterInMap = topicRouters.putIfAbsent(topicName, topicRouter);
        if (topicRouterInMap == null) {
            topicRouter.publisherAttached(publisher);
        }
        else {
            topicRouterInMap.publisherAttached(publisher);
        }
        log.debug("publisher attached to topic: " + topicName);
    }

    void publisherDetached(String topicName, CAMQPTargetInterface publisher, CAMQPEndpointPolicy endpointPolicy) {
        TopicRouterType routerType = endpointPolicy.getTopicRouterType();
        ConcurrentMap<String, TopicRouter> topicRouters = topicRoutersMap.get(routerType);

        TopicRouter topicRouter = topicRouters.get(topicName);
        if (topicRouter != null) {
            topicRouter.publisherDetached(publisher);
            if (topicRouter.isCompletelyDetached()) {
                log.debug("Removing topic: " + topicName);
                topicRouters.remove(topicName);
            }
        }
        log.debug("Publisher detached from topic: " + topicName);
    }

    void subscriberAttached(String topicName, CAMQPSourceInterface subscriber, CAMQPEndpointPolicy endpointPolicy) {
        TopicRouterType routerType = endpointPolicy.getTopicRouterType();

        if (routerType == TopicRouterType.Hierarchical) {
            endpointPolicy.setSubscriptionTopicHierarchy(topicName);
            topicName = getRootTopicName(topicName);
        }

        TopicRouter topicRouter = new TopicRouter(topicName, routerType);
        ConcurrentMap<String, TopicRouter> topicRouters = topicRoutersMap.get(routerType);
        TopicRouter topicRouterInMap = topicRouters.putIfAbsent(topicName, topicRouter);
        if (topicRouterInMap == null) {
            topicRouter.subscriberAttached(subscriber, endpointPolicy);
        }
        else {
            topicRouterInMap.subscriberAttached(subscriber, endpointPolicy);
        }
        log.debug("subscriber attached to topic: " + topicName);
    }

    void subscriberDetached(String topicName, CAMQPSourceInterface subscriber, CAMQPEndpointPolicy endpointPolicy) {
        TopicRouterType routerType = endpointPolicy.getTopicRouterType();
        ConcurrentMap<String, TopicRouter> topicRouters = topicRoutersMap.get(routerType);

        if (routerType == TopicRouterType.Hierarchical) {
            topicName = getRootTopicName(topicName);
        }

        TopicRouter topicRouter = topicRouters.get(topicName);
        if (topicRouter != null) {
            topicRouter.subscriberDetached(subscriber);
            if (topicRouter.isCompletelyDetached()) {
                log.debug("Removing topic: " + topicName);
                topicRouters.remove(topicName);
            }
        }
        log.debug("Subscriber detached from topic: " + topicName);
    }

    private String getRootTopicName(String topicName) {
        int pos = topicName.indexOf('.');
        if (pos != -1) {
            return topicName.substring(0, pos);
        } else {
            return topicName;
        }
    }

    /*
     * For junit test only
     */
    TopicRouter getTopicRouter(String topicName, TopicRouterType routerType) {
        ConcurrentMap<String, TopicRouter> topicRouters = topicRoutersMap.get(routerType);
        return topicRouters.get(topicName);
    }
}
