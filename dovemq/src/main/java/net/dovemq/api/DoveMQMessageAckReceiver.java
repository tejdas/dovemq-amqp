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

/**
 * An AMQP message sender (queue producer or topic publisher)
 * must implement this interface
 * to asynchronously receive acknowledgment indication
 * for a message that has been received and acknowledged
 * by a DoveMQMessageReceiver.
 *
 * @author tejdas
 */
public interface DoveMQMessageAckReceiver {
    public void messageAcknowledged(DoveMQMessage message);
}
