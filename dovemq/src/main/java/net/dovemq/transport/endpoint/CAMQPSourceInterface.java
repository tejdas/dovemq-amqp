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

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.link.CAMQPMessage;

/**
 * This interface represents a Source end-point, that is used by
 * Broker and Producer/Publisher to send messages.
 *
 * @author tdas
 *
 */
public interface CAMQPSourceInterface extends CAMQPEndpointInterface {
    /**
     * Called by Broker and (optionally) Producer/Publisher to register an
     * observer, to be notified when a message transitions to a terminal state.
     *
     * @param observer
     */
    public void registerDispositionObserver(CAMQPMessageDispositionObserver observer);

    /**
     * Sends a message on the underlying CAMQPSourceInterface.
     *
     * @param message
     *            DoveMQMessage to send.
     */
    public void sendMessage(DoveMQMessage message);

    /**
     * Called by Link Sender to asynchronously pull a message for sending.
     *
     * @return CAMQPMessage to send.
     */
    public CAMQPMessage getMessage();

    /**
     * Called by Link Sender to find out the number of messages waiting to be
     * sent.
     *
     * @return count of messages at Source.
     */
    public long getMessageCount();

    /**
     * Called by Link Sender just before the message is sent. Puts the message
     * in the unsettledDeliveries map if the message delivery policy is
     * ExactlyOnce or AtleastOnce. For, AtmostOnce, it just forgets about the
     * message after sending it.
     */
    public void messageSent(long deliveryId, CAMQPMessage message);

    /**
     * Called by Link Sender upon receipt of disposition control frame. Receives
     * a collection of deliveryIds corresponding to messages being disposed.
     * Processes the messages that are sent by this end-point, and returns back
     * a collection of unprocessed messages. The reason this happens is that,
     * for a session attached to multiple links, a batched disposition frame may
     * contain messages sent from different link end-points.
     *
     * @param deliveryIds
     *            : collection of deliveryIds for batched disposition of
     *            messages.
     * @param isMessageSettledByPeer
     *            : true/false.
     * @param newState
     * @return A Collection of deliveryIds for messages that are not sent from
     *         this end-point.
     */
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState);
}
