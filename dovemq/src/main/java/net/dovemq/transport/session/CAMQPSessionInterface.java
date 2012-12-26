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

package net.dovemq.transport.session;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Interface used by the link layer to send link control frames,
 * transfer and flow frames, and also to send acknowledge frame.
 * Implemented by CAMQPSession
 *
 * @author tejdas
 *
 */
public interface CAMQPSessionInterface {
    /**
     * Called by Link layer to send an encoded Link control frame.
     *
     * @param encodedLinkControlFrame
     */
    public void sendLinkControlFrame(ChannelBuffer encodedLinkControlFrame);

    /**
     * Called by Link layer to register a LinkReceiver to receive transfer
     * frames and Link Control frames. Incoming transfer and control frames are
     * dispatched to the LinkReceiver based on the linkHandle.
     *
     * @param linkHandle
     * @param linkReceiver
     */
    public void registerLinkReceiver(Long linkHandle, CAMQPLinkMessageHandler linkReceiver);

    /**
     * Called by LinkSender to get the next session-scoped delivery Id, to be
     * used to send a transfer frame.
     *
     * @return session-scoped deliveryId
     */
    public long getNextDeliveryId();

    /**
     * Called by LinkSender to send a transfer frame to the sender. After the
     * frame is sent, {@link CAMQPLinkSenderInterface#messageSent()} is called
     * to notify the link layer.
     *
     * @param transfer
     *            : transfer frame.
     * @param payload
     *            : encoded message payload performative.
     * @param linkSender
     *            : link sender that is notified upon transfer of the frame.
     */
    public void sendTransfer(CAMQPControlTransfer transfer, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender);

    /**
     * Called by Link end-point to send a link flow frame.
     *
     * @param flow
     */
    public void sendFlow(CAMQPControlFlow flow);

    /**
     * Called by Link receiver to acknowledge receipt of transfer frame.
     *
     * @param transferId
     */
    public void ackTransfer(long transferId);

    /**
     * Called by Link Endpoint to send disposition frame corresponding to the
     * transfer frame represented by deliveryId.
     *
     * @param deliveryId
     *            : deliveryId of the transfer frame being disposed.
     * @param settleMode
     *            : whether settled by the link end-point.
     * @param role
     *            : true (LinkReceiver) or false (LinkSender)
     * @param newState
     *            : new state that the frame transitioned to (accepted,
     *            modified, released, rejected)
     */
    public void sendDisposition(long deliveryId, boolean settleMode, boolean role, Object newState);

    /**
     * Close the session and notify the attached link end-points that the
     * session has ended.
     */
    public void close();
}
