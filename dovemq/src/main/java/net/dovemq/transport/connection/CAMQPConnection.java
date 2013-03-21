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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.session.CAMQPSessionFrameHandler;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

/**
 * AMQP Connection implementation on the outgoing side
 *
 *    ==>> CAMQPConnection ==>>
 * <<== CAMQPConnectionHandler <<==
 * @author tejdas
 *
 */
@ThreadSafe
final class CAMQPConnection implements CAMQPConnectionInterface {
    private final CAMQPConnectionStateActor stateActor;

    private CAMQPSender sender = null;

    @GuardedBy("stateActor")
    private final Map<Integer, CAMQPIncomingChannelHandler> incomingChannels = new HashMap<>();

    @GuardedBy("stateActor")
    private final boolean[] outgoingChannelsInUse = new boolean[CAMQPConnectionConstants.MAX_CHANNELS_SUPPORTED];

    private final CAMQPSessionFrameHandler sessionFrameHandler = CAMQPSessionFrameHandler.createInstance();

    public CAMQPSessionFrameHandler getSessionFrameHandler() {
        return sessionFrameHandler;
    }

    CAMQPConnection(CAMQPConnectionStateActor stateActor) {
        Arrays.fill(outgoingChannelsInUse, false);
        this.stateActor = stateActor;
        if (!stateActor.isInitiator) {
            sender = stateActor.sender;
            CAMQPConnectionHandler connectionHandler = sender.getChannel()
                    .getPipeline()
                    .get(CAMQPConnectionHandler.class);
            connectionHandler.registerConnection(this);
        }
    }

    /*
     * For Junit test
     */
    public CAMQPConnection() {
        stateActor = null;
    }

    @Override
    public CAMQPConnectionKey getKey() {
        return stateActor.key;
    }

    /**
     * Send an AMQP frame on the specified channel
     *
     * @param data
     * @param channelId
     */
    @Override
    public void sendFrame(ChannelBuffer data, int channelId) {
        ChannelBuffer header = CAMQPFrameHeader.createEncodedFrameHeader(channelId, data.readableBytes());
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, data), CAMQPFrameConstants.FRAME_TYPE_SESSION);
    }

    /**
     * Initiate AMQP connection handshake
     *
     * @param channel
     * @param connectionProps
     */
    void initialize(Channel channel, CAMQPConnectionProperties connectionProps) {
        stateActor.setChannel(channel);

        CAMQPConnectionHandler connectionHandler = channel.getPipeline()
                .get(CAMQPConnectionHandler.class);
        connectionHandler.registerConnection(this);

        stateActor.initiateHandshake(connectionProps);
    }

    /**
     * Wait for AMQP handshake to complete
     */
    void waitForReady() {
        stateActor.waitForOpenExchange();
        sender = stateActor.sender;
        CAMQPConnectionManager.connectionCreated(stateActor.key, this);
    }

    /**
     * Synchronously close the connection
     */
    @Override
    public void close() {
        stateActor.sendCloseControl();
        sender.waitForClose();
    }

    /**
     * Asynchronously close the connection
     */
    @Override
    public void closeAsync() {
        stateActor.sendCloseControl();
    }

    boolean isClosed() {
        return sender.isClosed();
    }

    /**
     * Called by the session layer to reserve an outgoing channel, which it will
     * subsequently attach to.
     *
     * @return
     */
    @Override
    public int reserveOutgoingChannel() {
        synchronized (stateActor) {
            int maxChannels = stateActor.getConnectionProps().getMaxChannels();
            if (!stateActor.canAttachChannels()) {
                throw new IllegalStateException("Not connected anymore; connection key: " + stateActor.key.toString());
            }
            /*
             * ChannelID 0 is reserved for Connection control
             */
            for (int i = 1; i < maxChannels; i++) {
                if (!outgoingChannelsInUse[i]) {
                    outgoingChannelsInUse[i] = true;
                    return i;
                }
            }
            throw new CAMQPConnectionException("All channels on this connection are in use; connection key: " + stateActor.key.toString());
        }
    }

    /**
     * Register an incoming channel with the channelHandler, so that the
     * incoming frames on the rxChannel could be dispatched.
     *
     * @param receiveChannelNumber
     * @param channelHandler
     */
    @Override
    public void register(int receiveChannelNumber, CAMQPIncomingChannelHandler channelHandler) {
        synchronized (stateActor) {
            if (stateActor.canAttachChannels()) {
                incomingChannels.put(receiveChannelNumber, channelHandler);
            } else {
                throw new IllegalStateException("Not connected anymore; connection key: " + stateActor.key.toString());
            }
        }
    }

    /**
     * Called by the session layer to detach itself from an outgoing channel.
     *
     * @param outgoingChannelNumber
     * @param incomingChannelNumber
     */
    @Override
    public void detach(int outgoingChannelNumber, int incomingChannelNumber) {
        synchronized (stateActor) {
            outgoingChannelsInUse[outgoingChannelNumber] = false;
            incomingChannels.remove(incomingChannelNumber);
        }
    }

    /**
     * Dispatches the incoming AMQP frame to the channelHandler associated with
     * the channelNumber (if it's already attached), or to
     * CAMQPSessionFrameHandler if it hasn't been attached yet.
     *
     * @param channelNumber
     * @param frame
     */
    void frameReceived(int channelNumber, CAMQPFrame frame) {
        CAMQPIncomingChannelHandler channelHandler = null;
        synchronized (stateActor) {
            channelHandler = incomingChannels.get(channelNumber);
        }

        if (channelHandler == null) {
            /*
             * The channel has not been attached yet. Dispatch to the
             * CAMQPSessionFrameHandler so it can attach a channelHandler to it.
             */
            sessionFrameHandler.frameReceived(channelNumber, frame, this);
        }
        else {
            channelHandler.frameReceived(frame);
        }
    }

    void aborted() {
        Collection<CAMQPIncomingChannelHandler> channelsToDetach = new ArrayList<>();

        synchronized (stateActor) {
            if (incomingChannels.size() > 0) {
                channelsToDetach.addAll(incomingChannels.values());
            }
        }
        for (CAMQPIncomingChannelHandler channelHandler : channelsToDetach) {
            channelHandler.channelAbruptlyDetached();
        }
        sender.close();
    }

    @Override
    public void registerSessionHandshakeInProgress(int sendChannelNumber, CAMQPSessionInterface session) {
        sessionFrameHandler.registerSessionHandshakeInProgress(sendChannelNumber, session);
    }
}
