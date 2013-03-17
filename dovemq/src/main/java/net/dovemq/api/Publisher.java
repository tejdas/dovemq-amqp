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

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

import org.apache.commons.lang.StringUtils;

/**
 * This class is used by publishers to publish an AMQP message
 * to a Topic.
 * It encapsulates an AMQP Link Sender.
 *
 * @author tejdas
 */
public final class Publisher extends BaseAckReceiver {
    private final boolean isHierarchicalPublisher;
    private final String topicHierarchy;

    private final CAMQPSourceInterface sourceEndpoint;

    /**
     * Publish a message.
     *
     * @param message
     */
    public void publishMessage(DoveMQMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("mesage cannot be null");
        }
        if (isHierarchicalPublisher) {
            if (StringUtils.isEmpty(message.getApplicationProperty(DoveMQMessageImpl.TOPIC_PUBLISH_HIERARCHY_KEY))) {
                message.setTopicPublishHierarchy(topicHierarchy);
            }
        }
        sourceEndpoint.sendMessage(message);
    }

    /**
     * Publish a binary payload as an AMQP message.
     *
     * @param payload
     */
    public void publishMessage(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }
        DoveMQMessage message = MessageFactory.createMessage();
        message.addPayload(payload);
        if (isHierarchicalPublisher) {
            message.setTopicPublishHierarchy(topicHierarchy);
        }
        sourceEndpoint.sendMessage(message);
    }

    Publisher(String topicHierarchy, CAMQPSourceInterface sourceEndpoint) {
        super();
        this.topicHierarchy = topicHierarchy;
        this.sourceEndpoint = sourceEndpoint;
        isHierarchicalPublisher = !(StringUtils.isEmpty(topicHierarchy));
        sourceEndpoint.registerDispositionObserver(this);
    }

    Publisher(CAMQPSourceInterface sourceEndpoint) {
        this(null, sourceEndpoint);
    }
}
