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

/**
 * This class is used by producers to send an AMQP message.
 * It encapsulates an AMQP Link Sender.
 *
 * @author tejdas
 */
public final class Producer extends BaseAckReceiver {
    private final CAMQPSourceInterface sourceEndpoint;

    /**
     * Send an AMQP message.
     *
     * @param message
     */
    public void sendMessage(DoveMQMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("mesage cannot be null");
        }
        sourceEndpoint.sendMessage(message);
    }

    /**
     * Send a binary payload as an AMQP message.
     *
     * @param payload
     */
    public void sendMessage(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }
        DoveMQMessage message = MessageFactory.createMessage();
        message.addPayload(payload);
        sourceEndpoint.sendMessage(message);
    }

    Producer(String sourceName, CAMQPSourceInterface sourceEndpoint) {
        super();
        this.sourceEndpoint = sourceEndpoint;
        sourceEndpoint.registerDispositionObserver(this);
    }
}
