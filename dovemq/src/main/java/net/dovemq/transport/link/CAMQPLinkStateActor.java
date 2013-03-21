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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.utils.CAMQPQueuedContext;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

enum State {
    DETACHED, ATTACH_SENT, ATTACH_RCVD, ATTACHED, DETACH_SENT, DETACH_RCVD, ATTACH_PIPE_DETACH_SENT, ATTACH_PIPE_DETACH_RCVD
}

enum Event {
    SEND_ATTACH, // API
    RECEIVED_ATTACH, // From Peer
    SEND_DETACH, // API
    RECEIVED_DETACH, // From Peer
    SESSION_UNMAPPED // From Session layer
}

/**
 * Acts on various state changes in AMQP link end-point during link
 * establishment and teardown.
 *
 * @author tdas
 */
@ThreadSafe
final class CAMQPLinkStateActor {
    private static class CAMQPLinkControlInfo {
        CAMQPLinkControlInfo(Object data) {
            super();
            this.data = data;
        }

        final Object data;

        boolean isInitiator = false;
    }

    private static final Logger log = Logger.getLogger(CAMQPLinkStateActor.class);

    private final CAMQPLinkEndpoint linkEndpoint;

    private boolean processingQueuedEvents = false;

    private final Queue<CAMQPQueuedContext<Event>> queuedEvents = new ConcurrentLinkedQueue<>();

    private volatile State currentState = State.DETACHED;

    boolean isLinkAttached() {
        return (currentState == State.ATTACHED);
    }

    CAMQPLinkStateActor(CAMQPLinkEndpoint linkEndpoint) {
        this.linkEndpoint = linkEndpoint;
    }

    void sendAttach(CAMQPControlAttach attachContext) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_ATTACH, new CAMQPLinkControlInfo(attachContext)));
        processEvents();
    }

    synchronized void waitForAttached(String targetAddress) {
        try {
            while (currentState != State.ATTACHED) {
                wait(CAMQPLinkConstants.LINK_HANDSHAKE_TIMEOUT);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (currentState != State.ATTACHED) {
            String errorMessage = "Timed out waiting for link to be attached to target address: " + targetAddress;
            log.warn(errorMessage);
            throw new CAMQPLinkException(errorMessage);
        }
    }

    void sendDetach(CAMQPControlDetach detachContext) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SEND_DETACH, new CAMQPLinkControlInfo(detachContext)));
        processEvents();
    }

    void sessionClosed() {
        preProcessSessionUnmapped(null);
    }

    synchronized void waitForDetached(String targetAddress) {
        try {
            while (currentState != State.DETACHED) {
                wait(CAMQPLinkConstants.LINK_HANDSHAKE_TIMEOUT);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (currentState != State.DETACHED) {
            String errorMessage = "Timed out waiting for link to be detached from target address: " + targetAddress;
            log.warn(errorMessage);
            throw new CAMQPLinkException(errorMessage);
        }
    }

    void attachReceived(CAMQPControlAttach data) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.RECEIVED_ATTACH, new CAMQPLinkControlInfo(data)));
        processEvents();
    }

    void detachReceived(CAMQPControlDetach data) {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.RECEIVED_DETACH, new CAMQPLinkControlInfo(data)));
        processEvents();
    }

    void sessionAbruptlyEnded() {
        queuedEvents.add(new CAMQPQueuedContext<Event>(Event.SESSION_UNMAPPED, null));
        processEvents();
    }

    @GuardedBy("this")
    private void preProcessAttachReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo attachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        if (currentState == State.DETACHED) {
            attachContext.isInitiator = false;
            currentState = State.ATTACH_RCVD;
        }
        else if (currentState == State.ATTACH_SENT) {
            attachContext.isInitiator = true;
        }
    }

    private CAMQPQueuedContext<Event> processAttachReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo attachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        synchronized (this) {
            if (attachContext.isInitiator && (currentState == State.ATTACH_SENT)) {
                currentState = State.ATTACHED;
                notifyAll();
            }
        }

        if (!attachContext.isInitiator) {
            linkEndpoint.processAttachReceived((CAMQPControlAttach) attachContext.data, attachContext.isInitiator);
        }
        else {
            linkEndpoint.attached(attachContext.isInitiator);
        }

        synchronized (this) {
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessSendAttach(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo attachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        if (currentState == State.DETACHED) {
            attachContext.isInitiator = true;
        }
        else if (currentState == State.ATTACH_RCVD) {
            attachContext.isInitiator = false;
        }
    }

    private CAMQPQueuedContext<Event> processSendAttach(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo attachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlAttach.encode(encoder, (CAMQPControlAttach) attachContext.data);

        linkEndpoint.getSession()
                .sendLinkControlFrame(encoder.getEncodedBuffer());

        synchronized (this) {
            if (currentState == State.DETACHED) {
                currentState = State.ATTACH_SENT;
            }
            else if (currentState == State.ATTACH_RCVD) {
                currentState = State.ATTACHED;
                notify();
            }
        }

        if (!attachContext.isInitiator) {
            linkEndpoint.attached(attachContext.isInitiator);
        }

        synchronized (this) {
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessDetachReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo detachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        if (currentState == State.ATTACHED) {
            currentState = State.DETACH_RCVD;
        }
        else if (currentState == State.DETACH_SENT) {
            detachContext.isInitiator = true;
        }
    }

    CAMQPQueuedContext<Event> processDetachReceived(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo detachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        synchronized (this) {
            if (detachContext.isInitiator && (currentState == State.DETACH_SENT)) {
                currentState = State.DETACHED;
                notify();
            }
        }
        if (detachContext.isInitiator) {
            linkEndpoint.detached(detachContext.isInitiator);
        }
        else {
            linkEndpoint.processDetachReceived((CAMQPControlDetach) detachContext.data, detachContext.isInitiator);
        }

        synchronized (this) {
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessSendDetach(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo detachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        if (currentState == State.ATTACHED) {
            detachContext.isInitiator = true;
        }
        else if (currentState == State.DETACH_RCVD) {
            detachContext.isInitiator = false;
        }
    }

    private CAMQPQueuedContext<Event> processSendDetach(CAMQPQueuedContext<Event> contextToProcess) {
        CAMQPLinkControlInfo detachContext = (CAMQPLinkControlInfo) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlDetach.encode(encoder, (CAMQPControlDetach) detachContext.data);
        linkEndpoint.getSession()
                .sendLinkControlFrame(encoder.getEncodedBuffer());

        synchronized (this) {
            if (currentState == State.ATTACHED) {
                currentState = State.DETACH_SENT;
            }
            else if (currentState == State.DETACH_RCVD) {
                currentState = State.DETACHED;
            }
        }

        if (!detachContext.isInitiator) {
            linkEndpoint.detached(detachContext.isInitiator);
        }

        synchronized (this) {
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessSessionUnmapped(CAMQPQueuedContext<Event> contextToProcess) {
        log.debug("Session: + detached abruptly");
        processingQueuedEvents = false;
        queuedEvents.clear();
        currentState = State.DETACHED;
    }

    private CAMQPQueuedContext<Event> processSessionUnmapped(CAMQPQueuedContext<Event> contextToProcess) {
        return null;
    }

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

            contextToProcess = processEvent(contextToProcess);
            if (contextToProcess == null) {
                return;
            }
        }
    }

    /*
     * Process current event and return next event off the queue
     */
    private CAMQPQueuedContext<Event> processEvent(CAMQPQueuedContext<Event> contextToProcess) {
        if (contextToProcess == null) {
            return null;
        }
        log.debug("CAMQPLinkStateActor.processEvent: " + contextToProcess.getEvent()
                .toString());
        if (contextToProcess.getEvent() == Event.SEND_ATTACH) {
            return processSendAttach(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_ATTACH) {
            return processAttachReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.SEND_DETACH) {
            return processSendDetach(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_DETACH) {
            return processDetachReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.SESSION_UNMAPPED) {
            return processSessionUnmapped(contextToProcess);
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
        if (eventToBeProcessed == Event.SEND_ATTACH) {
            if ((currentState != State.DETACHED) && (currentState != State.ATTACH_RCVD)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_ATTACH) {
            if ((currentState != State.ATTACH_SENT) && (currentState != State.DETACHED)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.SEND_DETACH) {
            if ((currentState != State.ATTACHED) && (currentState != State.DETACH_RCVD)) {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_DETACH) {
            if ((currentState != State.ATTACHED) && (currentState != State.DETACH_SENT)) {
                return false;
            }
        }
        return true;
    }

    @GuardedBy("this")
    private void processPreCondition(CAMQPQueuedContext<Event> contextToProcess) {
        Event eventToBeProcessed = contextToProcess.getEvent();
        if (eventToBeProcessed == Event.RECEIVED_ATTACH) {
            preProcessAttachReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.RECEIVED_DETACH) {
            preProcessDetachReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.SEND_ATTACH) {
            preProcessSendAttach(contextToProcess);
        }
        else if (eventToBeProcessed == Event.SEND_DETACH) {
            preProcessSendDetach(contextToProcess);
        }
        else if (eventToBeProcessed == Event.SESSION_UNMAPPED) {
            preProcessSessionUnmapped(contextToProcess);
        }
    }
}
