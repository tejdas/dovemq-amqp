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

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;

/**
 * CAMQPLinkSenderInterface provides methods for session and end-point layers
 * to interact with the link layer while sending message.
 *
 * @author tejdas
 *
 */
public interface CAMQPLinkSenderInterface {
    /**
     * Called by end-point layer to register a {@link CAMQPSourceInterface}.
     *
     * @param source
     *            : to be used for dispatching up the stack to the end-point
     *            layer.
     */
    public void registerSource(CAMQPSourceInterface source);

    /**
     * Called by Source end-point {@link CAMQPSourceInterface} to send an
     * encoded message.
     *
     * @param message
     */
    public void sendMessage(CAMQPMessage message);

    /**
     * Called by underlying {@link CAMQPSessionInterface} after a transfer frame
     * has been sent.
     *
     * @param transferFrame
     */
    public void messageSent(CAMQPControlTransfer transferFrame);

    public long getHandle();
}
