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

import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;

/**
 * This class is used by producers to send an AMQP message.
 * It encapsulates an AMQP Link Sender.
 *
 * @author tejdas
 */
public final class Producer implements CAMQPMessageDispositionObserver {
    private volatile DoveMQMessageAckReceiver ackReceiver = null;

    private final CAMQPSourceInterface sourceEndpoint;

    /**
     * Register a DoveMQMessageAckReceiver with the Producer, so that the sender
     * will be asynchronously notified of a message acknowledgment.
     *
     * @param ackReceiver
     */
    public void registerMessageAckReceiver(DoveMQMessageAckReceiver ackReceiver) {
        this.ackReceiver = ackReceiver;
    }

    /**
     * Send an AMQP message.
     *
     * @param message
     */
    public void sendMessage(DoveMQMessage message) {
        sourceEndpoint.sendMessage(message);
    }

    /**
     * Send a binary payload as an AMQP message.
     *
     * @param payload
     */
    public void sendMessage(byte[] payload) {
        DoveMQMessage message = MessageFactory.createMessage();
        message.addPayload(payload);
        sourceEndpoint.sendMessage(message);
    }

    Producer(String sourceName, CAMQPSourceInterface sourceEndpoint) {
        super();
        this.sourceEndpoint = sourceEndpoint;
        sourceEndpoint.registerDispositionObserver(this);
    }

    /**
     * Receives the message acknowledgment and forwards it to the registered
     * DoveMQMessageAckReceiver.
     */
    @Override
    public void messageAckedByConsumer(DoveMQMessage message, CAMQPSourceInterface source) {
        if (ackReceiver != null) {
            ackReceiver.messageAcknowledged(message);
        }
    }
}
