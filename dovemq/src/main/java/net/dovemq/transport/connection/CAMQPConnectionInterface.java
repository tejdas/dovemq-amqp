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

package net.dovemq.transport.connection;

import net.dovemq.transport.session.CAMQPSessionInterface;

import org.jboss.netty.buffer.ChannelBuffer;

public interface CAMQPConnectionInterface
{
    public int reserveOutgoingChannel();

    /**
     * Register an incoming channel with the channelHandler, so that
     * the incoming frames on the rxChannel could be dispatched.
     *
     * @param receiveChannelNumber
     * @param channelHandler
     */
    public void register(int receiveChannelNumber, CAMQPIncomingChannelHandler channelHandler);

    void registerSessionHandshakeInProgress(int sendChannelNumber, CAMQPSessionInterface session);

    /**
     * Called by the session layer to detach itself from an
     * outgoing channel.
     *
     * @param outgoingChannelNumber
     * @param incomingChannelNumber
     */
    public void detach(int outgoingChannelNumber, int incomingChannelNumber);

    /**
     * Send an AMQP frame on the specified channel
     * @param data
     * @param channelId
     */
    public void sendFrame(ChannelBuffer data, int channelId);

    public CAMQPConnectionKey getKey();

    /**
     * Synchronously close the connection
     */
    public void close();

    /**
     * Asynchronously close the connection
     */
    public void closeAsync();
}
