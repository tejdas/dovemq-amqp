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

abstract class BaseAckReceiver implements CAMQPMessageDispositionObserver {
    private volatile DoveMQMessageAckReceiver ackReceiver = null;

    /**
     * Register a DoveMQMessageAckReceiver with the Publisher, so that the
     * sender will be asynchronously notified of a message acknowledgment.
     *
     * @param ackReceiver
     */
    public void registerMessageAckReceiver(DoveMQMessageAckReceiver ackReceiver) {
        if (ackReceiver == null) {
            throw new IllegalArgumentException("Null ackReceiver specified");
        }
        this.ackReceiver = ackReceiver;
    }

    @Override
    public void messageAckedByConsumer(DoveMQMessage message, CAMQPSourceInterface source) {
        if (ackReceiver != null) {
            ackReceiver.messageAcknowledged(message);
        }
    }
}
