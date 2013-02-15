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

import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

/**
 * This class is used by consumers to receive and
 * (optionally) acknowledge the receipt of an AMQP message.
 * It encapsulates an AMQP Link Receiver.
 *
 * @author tejdas
 */
public final class Consumer implements CAMQPMessageReceiver {
    private final DoveMQEndpointPolicy endpointPolicy;

    private final CAMQPTargetInterface targetEndpoint;

    private volatile DoveMQMessageReceiver doveMQMessageReceiver = null;

    /**
     * Receives an AMQP message, and dispatches it to the registered
     * DoveMQMessageReceiver.
     */
    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target) {
        doveMQMessageReceiver.messageReceived(message);
    }

    /**
     * Register a DoveMQMessageReceiver with the Consumer, so that the receiver
     * will asynchronously receive AMQP messages.
     *
     * @param messageReceiver
     */
    public void registerMessageReceiver(DoveMQMessageReceiver messageReceiver) {
        if (messageReceiver == null) {
            throw new IllegalArgumentException("Null messageReceiver specified");
        }
        this.doveMQMessageReceiver = messageReceiver;
        targetEndpoint.registerMessageReceiver(this);
    }

    public void stop() {
    }

    /**
     * Called by the message receiver to explicitly acknowledge the receipt of
     * an AMQP message. This is relevant only when the acknowledgment policy is
     * CONSUMER_ACKS.
     *
     * @param message
     */
    public void acknowledge(DoveMQMessage message) {
        if (endpointPolicy.getAckPolicy() == MessageAcknowledgementPolicy.CONSUMER_ACKS) {
            long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
            targetEndpoint.acknowledgeMessageProcessingComplete(deliveryId);
        }
    }

    Consumer(String targetName, CAMQPTargetInterface targetEndpoint) {
        super();
        this.endpointPolicy = new DoveMQEndpointPolicy();
        this.targetEndpoint = targetEndpoint;
    }

    Consumer(String targetName,
            CAMQPTargetInterface targetEndpoint,
            DoveMQEndpointPolicy endpointPolicy) {
        super();
        this.endpointPolicy = endpointPolicy;
        this.targetEndpoint = targetEndpoint;
    }
}
