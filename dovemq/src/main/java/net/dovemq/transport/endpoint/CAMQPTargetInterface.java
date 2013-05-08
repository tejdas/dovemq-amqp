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

package net.dovemq.transport.endpoint;

import java.util.Collection;

import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;

/**
 * This interface represents a Target end-point, that is used by
 * Broker and Consumer/Subscriber to receive messages.
 *
 * @author tdas
 *
 */
public interface CAMQPTargetInterface extends CAMQPEndpointInterface {
    /**
     * Used by Broker and Consumer/Subscriber to register a
     * {@link CAMQPMessageReceiver} to receive messages.
     *
     * @param messageReceiver
     */
    public void registerMessageReceiver(CAMQPMessageReceiver messageReceiver);

    /**
     * Called by LinkReceiver upon receipt of a message.
     *
     * @param deliveryId
     * @param deliveryTag
     * @param message
     * @param settledBySender
     * @param receiverSettleMode
     */
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode);

    /**
     * Called by Link Sender upon receipt of disposition control frame. Receives
     * a collection of deliveryIds corresponding to messages being disposed.
     * Processes the messages that are received by this end-point, and returns
     * back a collection of unprocessed messages. The reason this happens is
     * that, for a session attached to multiple links, a batched disposition
     * frame may contain messages received by different link end-points.
     *
     * @param deliveryIds
     *            : collection of deliveryIds for batched disposition of
     *            messages.
     * @param isMessageSettledByPeer
     *            : true/false.
     * @param newState
     * @return A Collection of deliveryIds for messages that are not received by
     *         this end-point.
     */
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState);

    /**
     * Called by DoveMQMessageReceiver to acknowledge processing of a message.
     *
     * @param deliveryId
     */
    public void acknowledgeMessageProcessingComplete(long deliveryId);

    /**
     * Closes the underlying AMQP link with the error message
     * communicated to the peer link end-point.
     *
     * @param errorDetails
     */
    public void closeUnderlyingLink(CAMQPDefinitionError errorDetails);
}
