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
 * AMQP is a peer-peer transport protocol. DoveMQ provides
 * a programming model that allows a DoveMQ Sender endpoint
 * to talk to an AMQP link receiver, without the DoveMQ Broker.
 *
 * This class provides the functionality of a DoveMQ Channel
 * that encapsulates an AMQP link sender.
 */
public final class Channel extends BaseAckReceiver {
    private final CAMQPSourceInterface source;

    /**
     * Send an AMQP message.
     *
     * @param message
     */
    public void sendMessage(DoveMQMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("mesage cannot be null");
        }
        source.sendMessage(message);
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
        source.sendMessage(message);
    }

    Channel(String sourceName, CAMQPSourceInterface source) {
        super();
        this.source = source;
        source.registerDispositionObserver(this);
    }
}