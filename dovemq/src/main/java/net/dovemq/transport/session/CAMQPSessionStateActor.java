package net.dovemq.transport.session;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlEnd;

enum State
{
    UNMAPPED, BEGIN_SENT, BEGIN_RCVD, MAPPED, END_SENT, END_RCVD
}

enum Event
{
    SEND_BEGIN, // API
    RECEIVED_BEGIN, // From Peer
    SEND_END, // API
    RECEIVED_END, // From Peer
    CHANNEL_DETACHED
    // From Connection layer
}

class QueuedContext
{
    Event getEvent()
    {
        return event;
    }

    Object getContext()
    {
        return context;
    }

    QueuedContext(Event event, Object context)
    {
        super();
        this.event = event;
        this.context = context;
    }

    private final Event event;

    private final Object context;
}

@ThreadSafe
class CAMQPSessionStateActor
{
    private static final Logger log = Logger.getLogger(CAMQPSessionStateActor.class);

    private final CAMQPSession session;

    private boolean processingQueuedEvents = false;

    private final Queue<QueuedContext> queuedEvents = new ConcurrentLinkedQueue<QueuedContext>();

    private State currentState = State.UNMAPPED;

    State getCurrentState()
    {
        return currentState;
    }

    CAMQPSessionStateActor(CAMQPSession session)
    {
        this.session = session;
    }

    CAMQPSessionStateActor(CAMQPConnection attachedConnection)
    {
        this.session = new CAMQPSession(attachedConnection, this);
    }

    void sendBegin(CAMQPSessionControlWrapper beginContext)
    {
        queuedEvents.add(new QueuedContext(Event.SEND_BEGIN, beginContext));
        processEvents();
    }

    synchronized void waitForMapped()
    {
        try
        {
            while (currentState != State.MAPPED)
                wait();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    void sendEnd(CAMQPSessionControlWrapper endContext)
    {
        queuedEvents.add(new QueuedContext(Event.SEND_END, endContext));
        processEvents();
    }

    synchronized void waitForUnmapped()
    {
        try
        {
            while (currentState != State.UNMAPPED)
                wait();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    void beginReceived(CAMQPSessionControlWrapper data)
    {
        queuedEvents.add(new QueuedContext(Event.RECEIVED_BEGIN, data));
        processEvents();
    }

    void endReceived(CAMQPControlEnd data)
    {
        queuedEvents.add(new QueuedContext(Event.RECEIVED_END, new CAMQPSessionControlWrapper(data)));
        processEvents();
    }

    void channelAbruptlyDetached()
    {
        queuedEvents.add(new QueuedContext(Event.CHANNEL_DETACHED, null));
        processEvents();
    }

    @GuardedBy("this")
    private void preProcessBeginReceived(QueuedContext contextToProcess)
    {
        if (currentState == State.UNMAPPED)
        {
            currentState = State.BEGIN_RCVD;
        }
    }

    private QueuedContext processBeginReceived(QueuedContext contextToProcess)
    {
        CAMQPSessionControlWrapper beginContext = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPControlBegin beginControl = (CAMQPControlBegin) beginContext.getSessionControl();
        boolean isInitiator = isSessionInitiator(beginControl);
        if (!isInitiator)
        {
            session.beginReceived(beginContext);
        }
        else
        {
            session.mapped();
        }
        synchronized (this)
        {
            if (isInitiator && (currentState == State.BEGIN_SENT))
            {
                currentState = State.MAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    private QueuedContext processSendBegin(QueuedContext contextToProcess, CAMQPConnection attachedConnection)
    {
        CAMQPSessionControlWrapper beginContext = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();

        CAMQPControlBegin beginControl = (CAMQPControlBegin) beginContext.getSessionControl();
        boolean isInitiator = (beginControl.getRemoteChannel() == 0);

        CAMQPControlBegin.encode(encoder, beginControl);
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();

        attachedConnection.sendFrame(encodedControl, beginContext.getChannelNumber());

        if (!isInitiator)
        {
            session.mapped();
        }
        synchronized (this)
        {
            if (currentState == State.UNMAPPED)
            {
                currentState = State.BEGIN_SENT;
            }
            else if (currentState == State.BEGIN_RCVD)
            {
                currentState = State.MAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessEndReceived(QueuedContext contextToProcess)
    {
        CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        if (currentState == State.END_SENT)
        {
            data.setControlInitiator(true);
        }
        else if (currentState == State.MAPPED)
        {
            currentState = State.END_RCVD;
        }
    }

    QueuedContext processEndReceived(QueuedContext contextToProcess)
    {
        CAMQPSessionControlWrapper dataWrapper = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPControlEnd data = (CAMQPControlEnd) dataWrapper.getSessionControl();
        if (dataWrapper.isControlInitiator())
        {
            session.unmapped();
        }
        else
        {
            session.endReceived(data);
        }

        synchronized (this)
        {
            if (dataWrapper.isControlInitiator() && (currentState == State.END_SENT))
            {
                currentState = State.UNMAPPED;
                notify();
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessSendEnd(QueuedContext contextToProcess)
    {
        if (currentState == State.MAPPED)
        {
            CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
            data.setControlInitiator(true);
        }
    }

    private QueuedContext processSendEnd(QueuedContext contextToProcess, CAMQPConnection attachedConnection)
    {
        CAMQPSessionControlWrapper data = (CAMQPSessionControlWrapper) contextToProcess.getContext();
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlEnd.encode(encoder, (CAMQPControlEnd) data.getSessionControl());
        ChannelBuffer encodedControl = encoder.getEncodedBuffer();
        attachedConnection.sendFrame(encodedControl, data.getChannelNumber());

        if (!data.isControlInitiator())
        {
            session.unmapped();
        }
        synchronized (this)
        {
            if (currentState == State.MAPPED)
            {
                currentState = State.END_SENT;
            }
            else if (currentState == State.END_RCVD)
            {
                currentState = State.UNMAPPED;
            }
            return getNextEvent();
        }
    }

    @GuardedBy("this")
    private void preProcessChannelDetached(QueuedContext contextToProcess)
    {
        log.debug("Session: + detached abruptly");
        processingQueuedEvents = false;
        queuedEvents.clear();
        currentState = State.UNMAPPED;
    }

    private QueuedContext processChannelDetached(QueuedContext contextToProcess)
    {
        session.unmapped();
        return null;
    }

    private void processEvents()
    {
        boolean firstPass = true;
        QueuedContext contextToProcess = null;
        CAMQPConnection attachedConnection = null;
        while (true)
        {
            if (firstPass)
            {
                firstPass = false;
                synchronized (this)
                {
                    if (processingQueuedEvents)
                    {
                        return;
                    }
                    else
                    {
                        processingQueuedEvents = true;
                    }
                    contextToProcess = getNextEvent();
                    attachedConnection = session.getConnection();
                }
            }

            contextToProcess = processEvent(contextToProcess, attachedConnection);
            if (contextToProcess == null)
            {
                return;
            }
        }
    }

    /*
     * Process current event and return next event off the queue
     */
    private QueuedContext processEvent(QueuedContext contextToProcess, CAMQPConnection attachedConnection)
    {
        if (contextToProcess == null)
        {
            return null;
        }
        log.info("SessionActor.processEvent: " + contextToProcess.getEvent().toString());
        if (contextToProcess.getEvent() == Event.SEND_BEGIN)
        {
            if (attachedConnection != null)
            {
                return processSendBegin(contextToProcess, attachedConnection);
            }
            else
            {
                log.fatal("Could not process Event.SEND_ATTACH as underlying CAMQPConnection is null");
                // REVISIT TODO
                return null;
            }
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_BEGIN)
        {
            return processBeginReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.SEND_END)
        {
            if (attachedConnection != null)
            {
                return processSendEnd(contextToProcess, attachedConnection);
            }
            else
            {
                log.fatal("Could not process Event.SEND_DETACH as underlying CAMQPConnection is null");
                // REVISIT TODO
                return null;
            }
        }
        else if (contextToProcess.getEvent() == Event.RECEIVED_END)
        {
            return processEndReceived(contextToProcess);
        }
        else if (contextToProcess.getEvent() == Event.CHANNEL_DETACHED)
        {
            return processChannelDetached(contextToProcess);
        }
        return null;
    }

    @GuardedBy("this")
    private QueuedContext getNextEvent()
    {
        while (true)
        {
            if (queuedEvents.isEmpty())
            {
                processingQueuedEvents = false;
                return null;
            }

            QueuedContext contextToProcess = queuedEvents.remove();
            if (checkCurrentState(contextToProcess.getEvent()))
            {
                processPreCondition(contextToProcess);
                return contextToProcess;
            }
            else
            {
                log.fatal("Incorrect state detected: currentState: " + currentState + " Event to be processed: " + contextToProcess.getEvent());
            }
        }
    }

    @GuardedBy("this")
    private boolean checkCurrentState(Event eventToBeProcessed)
    {
        if (eventToBeProcessed == Event.SEND_BEGIN)
        {
            if ((currentState != State.UNMAPPED) && (currentState != State.BEGIN_RCVD))
            {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_BEGIN)
        {
            if ((currentState != State.BEGIN_SENT) && (currentState != State.UNMAPPED))
            {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.SEND_END)
        {
            if ((currentState != State.MAPPED) && (currentState != State.END_RCVD))
            {
                return false;
            }
        }
        else if (eventToBeProcessed == Event.RECEIVED_END)
        {
            if ((currentState != State.MAPPED) && (currentState != State.END_SENT))
            {
                return false;
            }
        }
        return true;
    }

    @GuardedBy("this")
    private void processPreCondition(QueuedContext contextToProcess)
    {
        Event eventToBeProcessed = contextToProcess.getEvent();
        if (eventToBeProcessed == Event.RECEIVED_BEGIN)
        {
            preProcessBeginReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.RECEIVED_END)
        {
            preProcessEndReceived(contextToProcess);
        }
        else if (eventToBeProcessed == Event.SEND_BEGIN)
        {
        }
        else if (eventToBeProcessed == Event.SEND_END)
        {
            preProcessSendEnd(contextToProcess);
        }
        else if (eventToBeProcessed == Event.CHANNEL_DETACHED)
        {
            preProcessChannelDetached(contextToProcess);
        }
    }

    private boolean isSessionInitiator(CAMQPControlBegin beginControl)
    {
        return (beginControl.getRemoteChannel() > 0);
    }
}
