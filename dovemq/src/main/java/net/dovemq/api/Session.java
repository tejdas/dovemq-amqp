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

package net.dovemq.api;

import java.util.regex.Pattern;

import net.dovemq.broker.endpoint.TopicRouterType;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.EndpointType;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.session.CAMQPSessionInterface;

import org.apache.commons.lang.StringUtils;

/**
 * This class represents an AMQP session.
 * It is also used as a factory to create
 * Producer, Consumer, Publisher and Subscriber.
 *
 * @author tejdas
 */
public final class Session
{
    private final String endpointId;
    private final CAMQPSessionInterface session;

    Session(String endpointId, CAMQPSessionInterface session)
    {
        super();
        this.endpointId = endpointId;
        this.session = session;
    }

    public void close()
    {
        session.close();
    }

    /**
     * Create a Producer and bind it to a transient queue
     * on the DoveMQ broker.
     *
     * @param queueName
     * @return
     */
    public Producer createProducer(String queueName)
    {
        String source = String.format("%s.%s", endpointId, queueName);
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(session, source, queueName, new CAMQPEndpointPolicy());
        return new Producer(source, sender);
    }

    /**
     * Create a Consumer and bind it to a transient queue
     * on the DoveMQ broker.
     *
     * @param queueName
     * @return
     */
    public Consumer createConsumer(String queueName)
    {
        String target = String.format("%s.%s", endpointId, queueName);
        CAMQPTargetInterface receiver = CAMQPEndpointManager.createTarget(session, queueName, target, new CAMQPEndpointPolicy());
        return new Consumer(target, receiver);
    }

    /**
     * Create a Consumer and bind it to a transient queue
     * on the DoveMQ broker.
     *
     * @param queueName
     * @return
     */
    public Consumer createConsumer(String queueName, DoveMQEndpointPolicy doveMQEndpointPolicy)
    {
        String target = String.format("%s.%s", endpointId, queueName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setDoveMQEndpointPolicy(doveMQEndpointPolicy);
        CAMQPTargetInterface receiver = CAMQPEndpointManager.createTarget(session, queueName, target, endpointPolicy);
        return new Consumer(target, receiver, doveMQEndpointPolicy);
    }

    /**
     * Create a Publisher and bind it to a Topic
     * on the DoveMQ broker.
     *
     * @param topicName
     * @return
     */
    public Publisher createPublisher(String topicName)
    {
        validateTopicName(topicName);
        String source = String.format("%s.%s", endpointId, topicName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(session, source, topicName, endpointPolicy);
        return new Publisher(topicName, sender);
    }

    /**
     * Create a Subscriber and bind it to Topic
     * on the DoveMQ broker.
     *
     * @param topicName
     * @return
     */
    public Subscriber createSubscriber(String topicName)
    {
        validateTopicName(topicName);
        String target = String.format("%s.%s", endpointId, topicName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        CAMQPTargetInterface receiver = CAMQPEndpointManager.createTarget(session, topicName, target, endpointPolicy);
        return new Subscriber(target, receiver);
    }

    /**
     * Create a Tag Filter Publisher and bind it to a Topic
     * on the DoveMQ broker.
     *
     * @param topicName
     * @return
     */
    public Publisher createTagFilterPublisher(String topicName)
    {
        validateTopicName(topicName);
        String source = String.format("%s.%s", endpointId, topicName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        endpointPolicy.setTopicRouterType(TopicRouterType.MessageTagFilter);
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(session, source, topicName, endpointPolicy);
        return new Publisher(topicName, sender);
    }

    /**
     * Create a subscriber and bind it to Topic.
     * Specify a messageFilterPattern, so that only
     * messages with a matching routing tag are forwarded
     * to this subscriber.
     *
     * @param topicName
     * @param messageFilterPattern
     * @return
     */
    public Subscriber createTagFilterSubscriber(String topicName, String messageFilterPattern)
    {
        validateTopicName(topicName);
        Pattern.compile(messageFilterPattern);
        String target = String.format("%s.%s", endpointId, topicName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        endpointPolicy.setTopicRouterType(TopicRouterType.MessageTagFilter);
        endpointPolicy.setMessageFilterPattern(messageFilterPattern);
        CAMQPTargetInterface receiver = CAMQPEndpointManager.createTarget(session, topicName, target, endpointPolicy);
        return new Subscriber(target, receiver);
    }

    /**
     * Create a Hierarchical Topic Publisher and bind it to a Topic
     * on the DoveMQ broker.
     *
     * @param topicRootName: root Topic name of the Hierarchy.
     * Example: sports
     * @return
     */
    public Publisher createHierarchicalTopicPublisher(String topicRootName)
    {
        validateTopicName(topicRootName);
        String source = String.format("%s.%s", endpointId, topicRootName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        endpointPolicy.setTopicRouterType(TopicRouterType.Hierarchical);
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(session, source, topicRootName, endpointPolicy);
        return new Publisher(topicRootName, sender);
    }

    /**
     * Create a Hierarchical Topic Subscriber and bind it to a Topic
     * on the DoveMQ broker.
     *
     * @param topicName: Hierarchical topic name.
     * Example: sports.baseball
     * @return
     */
    public Subscriber createHierarchicalTopicSubscriber(String topicName)
    {
        if (StringUtils.isEmpty(topicName))
        {
            throw new IllegalArgumentException("Topic name cannot be null or empty");
        }

        String target = String.format("%s.%s", endpointId, topicName);
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setEndpointType(EndpointType.TOPIC);
        endpointPolicy.setTopicRouterType(TopicRouterType.Hierarchical);
        CAMQPTargetInterface receiver = CAMQPEndpointManager.createTarget(session, topicName, target, endpointPolicy);
        return new Subscriber(target, receiver);
    }

    private static void validateTopicName(String topicName)
    {
        if (StringUtils.isEmpty(topicName))
        {
            throw new IllegalArgumentException("Topic name cannot be null or empty");
        }

        if (topicName.contains("."))
        {
            throw new IllegalArgumentException(". not allowed in topic name");
        }
    }
}
