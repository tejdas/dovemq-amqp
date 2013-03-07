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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlEnd;
import net.dovemq.transport.utils.CAMQPQueuedContext;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

enum State {
    UNMAPPED, BEGIN_SENT, BEGIN_RCVD, MAPPED, END_SENT, END_RCVD
}

enum Event {
    SEND_BEGIN, // API
    RECEIVED_BEGIN, // From Peer
    SEND_END, // API
    RECEIVED_END, // From Peer
    CHANNEL_DETACHED
    // From Connection layer
}

/**
 * Acts on various state changes in AMQP session.
 *
 * @author tdas
 */
@ThreadSafe
class CAMQPSessionStateActor {
    private static final Logger log = Logger.getLogger(CAMQPSessionStateActor.class);

    private final CAMQPSession session;

    private boolean processingQueuedEvents = false;

    private final Queue<CAMQPQueuedContext<Event>> queuedEvents = new ConcurrentLinkedQueue<>();

    private State currentState = State.UNMAPPED;

    State getCurrentState() {
        return currentState;
    }

    CAMQPSessionStateActor(CAMQPSession session) {
        this.session = session;
    }

    CAMQPSessionStateActor(CAMQPConnectionInterface attachedConnection) {
        this.session = new CAMQPSession(attachedConnection, this);
    }

    void sendBegin(CAMQPSessionControlWrapper beginContext) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_BEGIN, beginContext));
        processEvents();
    }

    synchronized void waitForMapped() {
        try {
            while (currentState != State.MAPPED) {
                wait();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void sendEnd(CAMQPSessionControlWrapper endContext) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_END, endContext));
        processEvents();
    }

    synchronized void waitForUnmapped() {
        try {
            while (currentState != State.UNMAPPED) {
                wait();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void beginReceived(CAMQPSessionControlWrapper data) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.RECEIVED_BEGIN, data));
        processEvents();
    }

    void endReceived(CAMQPControlEnd data) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.RECEIVED_END, new CAMQPSessionControlWrapper(data)));
        processEvents();
    }

    void channelAbruptlyDetached() {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.CHANNEL_DETACHED, null));
        processEvents();
    }

    @GuardedBy("this")
    private void preProcessBeginReceived(CAMQPQueuedContext<Event> contextToProcess) {
        if (currentState == State.UNMAPPED) {
            currentState = State.BEGIN_RCVD;
        }
    }

    private CAMQPQueuedContext<Event> processBeginReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPSessionControlWrapper beginContext = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPControlBegin beginControl = (CAMQPControlBegin) beginContext.getSessionControl();
        boolean isInitiator = isSessionInitiator(beginControl);
        if (!isInitiator) {
            session.beginReceived(beginContext);
        }
        else {
            session.mapped();
        }
        synchronized (this) {
            if (isInitiator && (currentState == State.BEGIN_SENT)) {
                currentState = State.MAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    private CAMQPQueuedContext<Event> processSendBegin(CAMQPQueuedContext<Event> contextToProcess, CAMQPConnectionInterface attachedConnection) {
        CAMQPSessionControlWrapper beginContext = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();

        CAMQPControlBegin beginControl = (CAMQPControlBegin) beginContext.getSessionControl();
        boolean isInitiator = (beginControl.getRemoteChannel() == 0);

        CAMQPControlBegin.encode(encoder, beginControl);
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();

        attachedConnection.sendFrame(encodedControl, beginContext.getChannelNumber());

        if (!isInitiator) {
            session.mapped();
        }
        synchronized (this) {
            if (currentState == State.UNMAPPED) {
                currentState = State.BEGIN_SENT;
            }
            else if (currentState == State.BEGIN_RCVD) {
                currentState = State.MAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessEndReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        if (currentState == State.END_SENT) {
            data.setControlInitiator(true);
        }
        else if (currentState == State.MAPPED) {
            currentState = State.END_RCVD;
        }
    }

    CAMQPQueuedContext<Event> processEndReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPSessionControlWrapper dataWrapper = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPControlEnd data = (CAMQPControlEnd) dataWrapper.getSessionControl();
        if (dataWrapper.isControlInitiator()) {
            session.unmapped();
        }
        else {
            session.endReceived(data);
        }

        synchronized (this) {
            if (dataWrapper.isControlInitiator() && (currentState == State.END_SENT)) {
                currentState = State.UNMAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessSendEnd(CAMQPQueuedContext<Event> contextToProcess) {
        if (currentState == State.MAPPED) {
            CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
            data.setControlInitiator(true);
        }
    }

    private CAMQPQueuedContext<Event> processSendEnd(CAMQPQueuedContext<Event> contextToProcess, CAMQPConnectionInterface attachedConnection) {
        CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlEnd.encode(encoder, (CAMQPControlEnd) data.getSessionControl());
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();
        attachedConnection.sendFrame(encodedControl, data.getChannelNumber());

        if (!data.isControlInitiator()) {
            session.unmapped();
        }
        synchronized (this) {
            if (currentState == State.MAPPED) {
                currentState = State.END_SENT;
            }
            else if (currentState == State.END_RCVD) {
                currentState = State.UNMAPPED;
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessChannelDetached(CAMQPQueuedContext<Event> contextToProcess) {
        log.debug("Session: + detached abruptly");
        processingQueuedEvents = false;
        queuedEvents.clear();
        currentState = State.UNMAPPED;
    }

    private CAMQPQueuedContext<Event> processChannelDetached(CAMQPQueuedContext<Event> contextToProcess) {
        session.unmapped();
        return null;
    }

    private void processEvents() {
        boolean firstPass = true;
        CAMQPQueuedContext<Event> contextToProcess = null;
        CAMQPConnectionInterface attachedConnection = null;
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
                    attachedConnection = session.getConnection();
                }
            }

            contextToProcess = processEvent(contextToProcess, attachedConnection);
            if (contextToProcess == null) {
                return;
            }
        }
    }

    /*
     * Process current event and return next event off the queue
     */
    private CAMQPQueuedContext<Event> processEvent(CAMQPQueuedContext<Event> contextToProcess, CAMQPConnectionInterface attachedConnection) {
        if (contextToProcess == null) {
            return null;
        }
        log.info("SessionActor.processEvent: " + contextToProcess.getEvent()
                .toString());
        if (contextToProcess.getEvent() == Event.SEND_BEGIN) {
            if (attachedConnection != null) {
                return processSendBegin(contextToProcess, attachedConnection);
            }
            else {
                log.fatal("Could not process Event.SEND_ATTACH as underlying CAMQPConnectionInterface is null");
                // TODO
                return null;
            }
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_BEGIN) {
            return processBeginReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.SEND_END) {
            if (attachedConnection != null) {
                return processSendEnd(contextToProcess, attachedConnection);
            }
            else {
                log.fatal("Could not process Event.SEND_DETACH as underlying CAMQPConnectionInterface is null");
                // TODO
                return null;
            }
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_END) {
            return processEndReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.CHANNEL_DETACHED) {
            return processChannelDetached(contextToProcess);
        }
        return null;
    }

    @GuardedBy("this")
    private CAMQPQueuedContext<Event> getNextEvent() {
        while (true) {
            if (queuedEvents.isEmpty()) {
                processingQueuedEvents = false;
                return null;
            }

            CAMQPQueuedContext<Event> contextToProcess = queuedEvents.remove();
            if (checkCurrentState(contextToProcess.getEvent())) {
                processPreCondition(contextToProcess);
                return contextToProcess;
            }
            else {
                log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
            }
        }
    }

    @GuardedBy("this")
    private boolean checkCurrentState(Event eventToBeProcessed) {
        if (eventToBeProcessed == Event.SEND_BEGIN) {
            if ((currentState != State.UNMAPPED) && (currentState != State.BEGIN_RCVD)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_BEGIN) {
            if ((currentState != State.BEGIN_SENT) && (currentState != State.UNMAPPED)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.SEND_END) {
            if ((currentState != State.MAPPED) && (currentState != State.END_RCVD)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_END) {
            if ((currentState != State.MAPPED) && (currentState != State.END_SENT)) {
                return false;
            }
        }
        return true;
    }

    @GuardedBy("this")
    private void processPreCondition(CAMQPQueuedContext<Event> contextToProcess) {
        Event eventToBeProcessed = contextToProcess.getEvent();
        if (eventToBeProcessed == Event.RECEIVED_BEGIN) {
            preProcessBeginReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.RECEIVED_END) {
            preProcessEndReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.SEND_END) {
            preProcessSendEnd(contextToProcess);
        }
        else if (eventToBeProcessed == Event.CHANNEL_DETACHED) {
            preProcessChannelDetached(contextToProcess);
        }
    }

    private boolean isSessionInitiator(CAMQPControlBegin beginControl) {
        return (beginControl.getRemoteChannel() > 0);
    }
}
