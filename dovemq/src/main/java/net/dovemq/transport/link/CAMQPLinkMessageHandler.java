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

package net.dovemq.transport.link;

import java.util.Collection;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

/**
 * Acts as an incoming entry point from Session layer to Link layer.
 * Represents a Link end-point.
 *
 * @author tdas
 *
 */
public interface CAMQPLinkMessageHandler
{
    /**
     * Called by Session layer upon receipt of a Link attach control frame.
     *
     * @param controlFrame
     */
    public void attachReceived(CAMQPControlAttach controlFrame);

    /**
     * Called by Session layer upon receipt of a Link detach control frame.
     *
     * @param controlFrame
     */
    public void detachReceived(CAMQPControlDetach controlFrame);

    /**
     * Called by Session layer upon receipt of a transfer frame.
     * Only applicable to Link Receiver.
     *
     * @param transferId: Transfer ID.
     * @param transferFrame: Transfer frame.
     * @param payload: Payload performative.
     */
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload);

    /**
     * Called by session layer upon receipt of a flow control frame.
     *
     * @param flow: Flow frame.
     */
    public void flowReceived(CAMQPControlFlow flow);

    /**
     * Called by Session layer upon receipt of disposition frame.
     *
     * @param deliveryIds: Collection of deliveryIds of messages for batched disposition.
     * @param isMessageSettledByPeer: true/false indicating if the message is settled by the peer.
     * @param newState: new message state.
     * @return
     */
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState);

    /**
     * Called by Session layer upon session closure.
     */
    public void sessionClosed();

    /**
     * Return the role of the linkEndpoint: Sender or Receiver.
     * Used by the Session layer to correctly dispatch the disposition frame based on the role.
     * @return
     */
    public LinkRole getRole();
}
