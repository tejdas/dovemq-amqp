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

import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

/**
 * This class is used by subscribers to receive an
 * AMQP message from a Topic.
 * It encapsulates an AMQP Link Receiver.
 *
 * @author tejdas
 */
public final class Subscriber implements CAMQPMessageReceiver {
    private final CAMQPTargetInterface targetEndpoint;

    private volatile DoveMQMessageReceiver doveMQMessageReceiver = null;

    /**
     * Receives an AMQP message and forwards it to a registered
     * DoveMQMessageReceiver;
     */
    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target) {
        doveMQMessageReceiver.messageReceived(message);
    }

    /**
     * Register a DoveMQMessageReceiver to asynchronously receive AMQP messages.
     *
     * @param messageReceiver
     */
    public void registerMessageReceiver(DoveMQMessageReceiver messageReceiver) {
        this.doveMQMessageReceiver = messageReceiver;
        targetEndpoint.registerMessageReceiver(this);
    }

    public Subscriber(String targetName, CAMQPTargetInterface targetEndpoint) {
        super();
        this.targetEndpoint = targetEndpoint;
    }
}
