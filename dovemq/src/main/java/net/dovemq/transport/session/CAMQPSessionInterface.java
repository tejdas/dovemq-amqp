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

import java.util.Collection;

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
public interface CAMQPSessionInterface
{
    public void sendLinkControlFrame(ChannelBuffer encodedLinkControlFrame);
    public void registerLinkReceiver(Long linkHandle, CAMQPLinkMessageHandler linkReceiver);
    public long getNextDeliveryId();
    public void sendTransfer(CAMQPControlTransfer transfer, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender);
    public void sendFlow(CAMQPControlFlow flow);
    public void ackTransfer(long transferId);
    public void sendDisposition(long deliveryId, boolean settleMode, boolean role, Object newState);
    public void sendBatchedDisposition(Collection<Long> deliveryIds, boolean settleMode, boolean role, Object newState);
    public void close();
}
