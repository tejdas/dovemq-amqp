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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlClose;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;
import net.dovemq.transport.utils.CAMQPQueuedContext;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

enum Event {
    SEND_CONN_HEADER_REQUESTED, // API
    SEND_CONN_HEADER, // Generated
    CONN_HDR_BYTES_RECEIVED, // From Peer
    CONN_HEADER_RECEIVED, // Generated
    MALFORMED_CONN_HEADER_RECEIVED, // Generated
    SEND_OPEN_REQUESTED, // API
    SEND_OPEN, // Generated
    OPENED, // Generated
    OPEN_RECEIVED, // From Peer
    SEND_CLOSE_REQUESTED, // API
    CLOSE_RECEIVED, // From Peer
    CLOSED, // Generated
    CONNECTION_ABORTED, // Generated,
    HEARTBEAT_DELAYED // Generated
}

enum State {
    START, HDR_RCVD, HDR_SENT, HDR_EXCH, OPEN_RCVD, OPEN_SENT, OPEN_PIPE, CLOSE_PIPE, OC_PIPE, OPENED, CLOSE_RCVD, CLOSE_SENT, END
}

/**
 * Acts on various state changes in AMQP connection
 * @author tejdas
 *
 */
@ThreadSafe
class CAMQPConnectionStateActor {
    private static final Logger log = Logger.getLogger(CAMQPConnectionStateActor.class);

    private volatile CAMQPHeartbeatProcessor heartbeatProcessor = null;

    private CAMQPConnectionProperties connectionProps = null;

    CAMQPConnectionProperties getConnectionProps() {
        return connectionProps;
    }

    boolean isInitiator;

    volatile CAMQPSender sender = null;

    void setChannel(Channel channel) {
        sender = new CAMQPSender(channel);
        key.setAddress(channel);
        heartbeatProcessor = new CAMQPHeartbeatProcessor(this, sender);
    }

    private boolean processingQueuedEvents = false;

    private final Queue<CAMQPQueuedContext<Event>> queuedEvents = new ConcurrentLinkedQueue<>();

    private final Queue<CAMQPQueuedContext<Event>> queuedGeneratedEvents = new ConcurrentLinkedQueue<>();

    @GuardedBy("this")
    private State currentState = State.START;

    @GuardedBy("this")
    private boolean openExchangeComplete = false;

    private synchronized boolean isOpenExchangeComplete() {
        return openExchangeComplete;
    }

    final CAMQPConnectionKey key = new CAMQPConnectionKey();

    CAMQPConnectionStateActor(boolean isInitiator,
            CAMQPConnectionProperties connectionProps) {
        this.isInitiator = isInitiator;
        this.connectionProps = connectionProps;
    }

    void initiateHandshake(CAMQPConnectionProperties connectionProps) {
        synchronized (this) {
            this.connectionProps = connectionProps;
        }
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_CONN_HEADER_REQUESTED, null));
        processEvents();
    }

    synchronized void waitForOpenExchange() {
        long now = System.currentTimeMillis();
        long expiryTime = now + CAMQPConnectionConstants.CONNECTION_HANDSHAKE_TIMEOUT;

        while (!openExchangeComplete && (now < expiryTime)) {
            try {
                wait(expiryTime - now);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            now = System.currentTimeMillis();
        }

        if (!openExchangeComplete) {
            throw new CAMQPConnectionException("Timed out waiting for connection handshake to complete");
        }
    }

    void sendOpenControl(CAMQPControlOpen openControlData) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_OPEN_REQUESTED, openControlData));
        processEvents();
    }

    void sendCloseControl() {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_CLOSE_REQUESTED, new CAMQPControlClose()));
        processEvents();
    }

    void notifyHeartbeatDelay() {
        queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.HEARTBEAT_DELAYED, null));
        processEvents();
    }

    void openControlReceived(CAMQPControlOpen peerConnectionProps) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.OPEN_RECEIVED, peerConnectionProps));
        processEvents();
    }

    void closeControlReceived(CAMQPControlClose closeContext) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.CLOSE_RECEIVED, closeContext));
        processEvents();
    }

    void heartbeatReceived() {
        heartbeatProcessor.receivedHeartbeat();
    }

    void connectionHeaderBytesReceived(ChannelBuffer buffer) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.CONN_HDR_BYTES_RECEIVED, buffer));
        processEvents();
    }

    void disconnectReceived() {
        queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CONNECTION_ABORTED, null));
        processEvents();
    }

    /**
     * Event processing loop. processXYZ() method processes the current context
     * and calls getNextEvent() to return the next context to process.
     */
    private void processEvents() {
        boolean firstPass = true;
        CAMQPQueuedContext<Event> contextToProcess = null;
        while (true) {
            if (firstPass) {
                firstPass = false;
                synchronized (this) {
                    if (processingQueuedEvents) {
                        return;
                    }
                    else {
                        processingQueuedEvents = true;
                    }
                    contextToProcess = getNextEvent();
                }
            }

            if (contextToProcess == null) {
                return;
            }

            log.debug(isInitiator + "   : processEvents: " + contextToProcess.getEvent()
                    .toString());
            if (contextToProcess.getEvent() == Event.SEND_CONN_HEADER_REQUESTED) {
                contextToProcess = processSendConnHeaderRequested(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.CONN_HDR_BYTES_RECEIVED) {
                contextToProcess = processConnHdrBytesReceived(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.SEND_CONN_HEADER) {
                contextToProcess = processSendConnHeader(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.SEND_OPEN_REQUESTED) {
                contextToProcess = processSendOpenRequested(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.SEND_OPEN) {
                contextToProcess = processSendOpen(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.OPEN_RECEIVED) {
                contextToProcess = processOpenReceived(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.OPENED) {
                contextToProcess = processOpened(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.SEND_CLOSE_REQUESTED) {
                contextToProcess = processSendCloseRequested(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.CLOSE_RECEIVED) {
                contextToProcess = processCloseReceived(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.CLOSED) {
                contextToProcess = processClosed(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.MALFORMED_CONN_HEADER_RECEIVED) {
                contextToProcess = processMalformedConnHeaderReceived(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.CONNECTION_ABORTED) {
                contextToProcess = processConnectionAborted(contextToProcess);
            }
            else if (contextToProcess.getEvent() == Event.HEARTBEAT_DELAYED) {
                contextToProcess = processHeartbeatDelayed(contextToProcess);
            }
        }
    }

    private CAMQPQueuedContext<Event> processSendConnHeaderRequested(CAMQPQueuedContext<Event> contextToProcess) {
        byte[] amqpHeader = CAMQPHeaderUtil.composeAMQPHeader();
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(amqpHeader), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        synchronized (this) {
            if (currentState == State.START) {
                currentState = State.HDR_SENT;
            }
            else {
                log.error("Connection was expected to be in State.START, but in bad state: " + currentState + " Connection key: " + key.toString());
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processSendConnHeader(CAMQPQueuedContext<Event> contextToProcess) {
        byte[] amqpHeader = CAMQPHeaderUtil.composeAMQPHeader();
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(amqpHeader), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        synchronized (this) {
            if (currentState == State.HDR_RCVD) {
                currentState = State.HDR_EXCH;

                CAMQPControlOpen controlOpen = initializeControlOpen(CAMQPConnectionManager.getContainerId());

                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_OPEN, controlOpen));
            }
            else if (currentState == State.END) {
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CLOSED, null));
            }
            else {
                log.error("Connection is in bad state: " + currentState + " Connection key: " + key.toString());
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processConnHdrBytesReceived(CAMQPQueuedContext<Event> contextToProcess) {
        ChannelBuffer buffer = (ChannelBuffer) contextToProcess.getContext();
        byte[] receivedAMQPHeader = new byte[buffer.readableBytes()];
        buffer.getBytes(0, receivedAMQPHeader);

        if (CAMQPHeaderUtil.validateAMQPHeader(receivedAMQPHeader)) {
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CONN_HEADER_RECEIVED, null));
        }
        else {
            log.error("MalformedHeader received: " + new String(receivedAMQPHeader));
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.MALFORMED_CONN_HEADER_RECEIVED, null));
        }

        synchronized (this) {
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void processPreconditionConnHeaderReceived(CAMQPQueuedContext<Event> contextToProcess) {
        if (currentState == State.START) {
            currentState = State.HDR_RCVD;
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_CONN_HEADER, null));
        }
        else if (currentState == State.HDR_SENT) {
            CAMQPControlOpen controlOpen = initializeControlOpen(CAMQPConnectionManager.getContainerId());

            currentState = State.HDR_EXCH;
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_OPEN, controlOpen));
        }
        else if (currentState == State.OPEN_PIPE) {
            currentState = State.OPEN_SENT;

        }
        else if (currentState == State.OC_PIPE) {
            currentState = State.CLOSE_PIPE;

        }
        else {
            log.error("Connection is in bad state: " + currentState + " Connection key: " + key.toString());
        }
    }

    private CAMQPQueuedContext<Event> processMalformedConnHeaderReceived(CAMQPQueuedContext<Event> contextToProcess) {
        if (!isInitiator) {
            byte[] amqpHeader = CAMQPHeaderUtil.composeAMQPHeader();
            sender.sendBuffer(ChannelBuffers.wrappedBuffer(amqpHeader), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        }
        synchronized (this) {
            currentState = State.END;
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CLOSED, null));
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processSendOpenRequested(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlOpen.encode(encoder, (CAMQPControlOpen) contextToProcess.getContext());
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();

        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
        frameHeader.setChannelNumber((short) 0);
        frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE + encodedControl.readableBytes());

        ChannelBuffer header = CAMQPFrameHeaderCodec.encode(frameHeader);
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, encodedControl), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        synchronized (this) {
            if (currentState == State.HDR_SENT) {
                currentState = State.OPEN_PIPE;
            }
            else {
                log.error("Connection is in bad state: " + currentState + " Connection key: " + key.toString());
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processSendOpen(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlOpen.encode(encoder, (CAMQPControlOpen) contextToProcess.getContext());
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();

        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
        frameHeader.setChannelNumber((short) 0);
        frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE + encodedControl.readableBytes());

        ChannelBuffer header = CAMQPFrameHeaderCodec.encode(frameHeader);
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, encodedControl), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        synchronized (this) {
            if (currentState == State.HDR_EXCH) {
                currentState = State.OPEN_SENT;
            }
            else if (currentState == State.OPEN_RCVD) {
                currentState = State.OPENED;
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.OPENED, null));
            }
            else {
                log.error("Connection is in bad state: " + currentState + " Connection key: " + key.toString());
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processOpened(CAMQPQueuedContext<Event> contextToProcess) {
        if (!isInitiator) {
            CAMQPConnectionManager.connectionAccepted(this, key);
        }

        heartbeatProcessor.scheduleNextHeartbeat();
        synchronized (this) {
            openExchangeComplete = true;
            if (isInitiator) {
                notify();
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processOpenReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPControlOpen peerOpenControlData = (CAMQPControlOpen) contextToProcess.getContext();
        key.setRemoteContainerId(peerOpenControlData.getContainerId());
        synchronized (this) {
            if (currentState == State.HDR_EXCH) {
                connectionProps.update(peerOpenControlData);
                CAMQPControlOpen controlOpen = initializeControlOpen(CAMQPConnectionManager.getContainerId());

                currentState = State.OPEN_RCVD;
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_OPEN, controlOpen));
            }
            else if (currentState == State.OPEN_SENT) {
                currentState = State.OPENED;
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.OPENED, null));
            }
            else if (currentState == State.CLOSE_PIPE) {
                currentState = State.CLOSE_SENT;
            }
            else if (currentState == State.HDR_SENT) {
                connectionProps.update(peerOpenControlData);
                CAMQPControlOpen controlOpen = initializeControlOpen(CAMQPConnectionManager.getContainerId());

                currentState = State.OPEN_RCVD;
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_OPEN, controlOpen));
            }
            else {
                log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processSendCloseRequested(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlClose ctx = (CAMQPControlClose) contextToProcess.getContext();
        CAMQPControlClose.encode(encoder, ctx);
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();

        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
        frameHeader.setChannelNumber((short) 0);
        frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE + encodedControl.readableBytes());

        ChannelBuffer header = CAMQPFrameHeaderCodec.encode(frameHeader);
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, encodedControl), CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        synchronized (this) {
            if (currentState == State.OPENED) {
                currentState = State.CLOSE_SENT;
            }
            else if (currentState == State.OPEN_PIPE) {
                currentState = State.OC_PIPE;
            }
            else if (currentState == State.OPEN_SENT) {
                currentState = State.CLOSE_PIPE;
            }
            else if (currentState == State.CLOSE_RCVD) {
                currentState = State.END;
                queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CLOSED, null));
            }
            else {
                log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void processPreconditionCloseReceived(CAMQPQueuedContext<Event> contextToProcess) {
        if (currentState == State.OPENED) {
            currentState = State.CLOSE_RCVD;
        }
        else if (currentState == State.CLOSE_SENT) {
            currentState = State.END;
            queuedGeneratedEvents.add(new CAMQPQueuedContext<Event>(Event.CLOSED, null));
        }
        else {
            log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
        }
    }

    private CAMQPQueuedContext<Event> processClosed(CAMQPQueuedContext<Event> contextToProcess) {
        cancelHeartbeat();
        sender.close();
        if (isOpenExchangeComplete()) {
            CAMQPConnectionManager.connectionClosed(key);
        }
        synchronized (this) {
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processCloseReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPConnectionManager.connectionCloseInitiatedByRemotePeer(key);
        synchronized (this) {
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processConnectionAborted(CAMQPQueuedContext<Event> contextToProcess) {
        cancelHeartbeat();
        if (isOpenExchangeComplete()) {
            CAMQPConnectionManager.connectionAborted(key);
        }
        synchronized (this) {
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processHeartbeatDelayed(CAMQPQueuedContext<Event> contextToProcess) {
        log.warn("Heartbeat delayed from peer. Closing AMQP connection: " + key.toString());
        cancelHeartbeat();
        if (isOpenExchangeComplete()) {
            CAMQPConnectionManager.connectionAborted(key);
        }
        synchronized (this) {
            return getNextEvent();
        }
    }

    /**
     * Loops until it gets the next context event that could be processed. If it
     * cannot be processed, it is ignored. Otherwise, it first processes the
     * pre-condition, and if there's nothing else that needs to be done, it
     * continues the loop, else returns the context to be processed.
     *
     * @return
     */
    @GuardedBy("this")
    private CAMQPQueuedContext<Event> getNextEvent() {
        while (true) {
            CAMQPQueuedContext<Event> contextToProcess = null;
            if (!queuedGeneratedEvents.isEmpty()) {
                contextToProcess = queuedGeneratedEvents.remove();
            }
            else if (!queuedEvents.isEmpty()) {
                contextToProcess = queuedEvents.remove();
            }

            if (contextToProcess == null) {
                processingQueuedEvents = false;
                return null;
            }

            if (isOKToProcessEvent(contextToProcess.getEvent())) {
                boolean processingComplete = processPreCondition(contextToProcess);
                if (!processingComplete) {
                    return contextToProcess;
                }
            }
            else {
                log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
            }
        }
    }

    @GuardedBy("this")
    private boolean isOKToProcessEvent(Event eventToBeProcessed) {
        if (eventToBeProcessed == Event.SEND_CONN_HEADER_REQUESTED) {
            return (currentState == State.START);
        }
        else if (eventToBeProcessed == Event.SEND_CONN_HEADER) {
            return (currentState == State.HDR_RCVD);
        }
        else if ((eventToBeProcessed == Event.CONN_HDR_BYTES_RECEIVED) || (eventToBeProcessed == Event.CONN_HEADER_RECEIVED)) {
            return ((currentState == State.START) || (currentState == State.HDR_SENT) || (currentState == State.OPEN_PIPE) || (currentState == State.OC_PIPE));
        }
        else if (eventToBeProcessed == Event.SEND_OPEN_REQUESTED) {
            return (currentState == State.HDR_SENT);
        }
        else if (eventToBeProcessed == Event.SEND_OPEN) {
            return ((currentState == State.HDR_EXCH) || (currentState == State.OPEN_RCVD));
        }
        else if (eventToBeProcessed == Event.OPEN_RECEIVED) {
            return ((currentState == State.HDR_EXCH) || (currentState == State.OPEN_SENT) || (currentState == State.CLOSE_PIPE) || (currentState == State.HDR_SENT));
        }
        else if (eventToBeProcessed == Event.SEND_CLOSE_REQUESTED) {
            return ((currentState == State.OPENED) || (currentState == State.OPEN_PIPE) || (currentState == State.OPEN_SENT) || (currentState == State.CLOSE_RCVD));
        }
        else if (eventToBeProcessed == Event.CLOSE_RECEIVED) {
            return ((currentState == State.OPENED) || (currentState == State.CLOSE_SENT));
        }
        return true;
    }

    @GuardedBy("this")
    private boolean processPreCondition(CAMQPQueuedContext<Event> contextToProcess) {
        Event eventToBeProcessed = contextToProcess.getEvent();
        boolean processingComplete = false;
        if (eventToBeProcessed == Event.CONN_HEADER_RECEIVED) {
            processPreconditionConnHeaderReceived(contextToProcess);
            processingComplete = true;
        }
        else if (eventToBeProcessed == Event.SEND_CLOSE_REQUESTED) {
            if (currentState == State.CLOSE_SENT) {
                /*
                 * prevent double-close
                 */
                log.warn("Connection.close control has already been sent, or queued for send");
                processingComplete = true;
            }
        }
        else if (eventToBeProcessed == Event.CLOSE_RECEIVED) {
            processPreconditionCloseReceived(contextToProcess);
            processingComplete = (currentState == State.END);
        }
        else if (eventToBeProcessed == Event.CONNECTION_ABORTED) {
            if ((currentState == State.END) || (currentState == State.START)) {
                log.info("Connection gracefully disconnected");
                processingComplete = true;
            }
            else {
                log.warn("Connection abortively disconnected");
                currentState = State.END;
            }
        }
        else if (eventToBeProcessed == Event.HEARTBEAT_DELAYED) {
            if ((currentState == State.END) || (currentState == State.START)) {
                processingComplete = true;
            }
            else {
                currentState = State.END;
            }
        }
        return processingComplete;
    }

    private void cancelHeartbeat() {
        heartbeatProcessor.stop();
        heartbeatProcessor = null;
    }

    private CAMQPControlOpen initializeControlOpen(String containerId) {
        CAMQPControlOpen controlOpen = new CAMQPControlOpen();
        controlOpen.setContainerId(containerId);
        controlOpen.setIdleTimeOut(connectionProps.getHeartbeatInterval());
        controlOpen.setChannelMax(connectionProps.getMaxChannels());
        controlOpen.setMaxFrameSize(connectionProps.getMaxFrameSizeSupported());
        return controlOpen;
    }

    @GuardedBy("this")
    boolean canAttachChannels() {
        return (currentState == State.OPENED);
    }
}
