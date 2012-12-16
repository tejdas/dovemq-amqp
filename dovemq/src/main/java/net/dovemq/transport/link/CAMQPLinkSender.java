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
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

/**
 * Implements AMQP Link Sender.
 * @author tejdas
 *
 */
@ThreadSafe
class CAMQPLinkSender extends CAMQPLinkEndpoint implements CAMQPLinkSenderInterface, Runnable
{
    private static final Logger log = Logger.getLogger(CAMQPLinkSender.class);

    private volatile CAMQPSourceInterface source = null;

    @Override
    public void registerSource(CAMQPSourceInterface source)
    {
        this.source = source;
    }

    /**
     * Messages that are waiting to be sent because no link credit is available,
     * or there is a send in progress.
     */
    private final ConcurrentLinkedQueue<CAMQPMessage> unsentMessages = new ConcurrentLinkedQueue<CAMQPMessage>();

    /**
     * Used to keep track of how many unsent messages are outstanding at session for this link.
     * A non-zero value is an indication that session is under flow-control.
     *
     * Incremented before call to CAMQPSessionInterface.sendTransfer()
     * Decremented in the callback from session layer: messageSent()
     *
     * Flow-control calculation in the link layer takes this value into account.
     * If linkCredit <= unsentMessagesAtSession, the link layer is under flow-control.
     *
     */
    private long unsentMessagesAtSession = 0;

    private boolean sendInProgress = false;

    /**
     * Remembers when the next flow frame needs to be explicitly sent,
     * asking for more link credit.
     */
    private long nextCreditRequestTime = -1;
    /**
     * Helps sending subsequent flow-frames with exponential back-off
     * period.
     */
    private int incrementedCount = 0;

    /**
     * Set to true if drain is requested by the peer and until it is processed.
     */
    private boolean drainRequested = false;

    private long maxAvailableLimit;

    public synchronized void setMaxAvailableLimit(long maxAvailableLimit)
    {
        this.maxAvailableLimit = maxAvailableLimit;
    }

    private volatile boolean receivedFirstFlowCredit = false;

    public CAMQPLinkSender(CAMQPSessionInterface session)
    {
        super(session);
        maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        // TODO error condition : should never be called for CAMQPLinkSender
    }

    /**
     * Called by Session layer when a flow frame is received from the peer.
     * Processes the flow frame:
     *
     *  (a) updates the link credit, taking into account the delta between
     *  Link Sender's deliveryCount and Link Receiver's delivery count.
     *
     *  (b) If there are unsent messages waiting for link credit to become
     *  available and there is available link credit (after it was updated),
     *  then executes a Runnable to start sending messages.
     *
     *  (c) If there is drain requested or echo flow requested, then process
     *  and sends a flow frame.
     */
    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        boolean messagesParked = false;
        CAMQPControlFlow outgoingFlow = null;
        synchronized (this)
        {
            if (flow.isSetLinkCredit() && (flow.getLinkCredit() >= 0))
            {
                if (flow.isSetDeliveryCount())
                {
                    linkCredit = flow.getLinkCredit() - (deliveryCount - flow.getDeliveryCount());
                }
                else
                {
                    linkCredit = flow.getLinkCredit() - deliveryCount;
                }
            }

            if (!receivedFirstFlowCredit && (linkCredit > 0))
            {
                /*
                 * Received the first flow-frame with non-zero
                 * link credit. Notify the thread waiting for
                 * link credit after the link establishment.
                 */
                receivedFirstFlowCredit = true;
                this.notifyAll();
            }

            /*
             * Reset nextCreditRequestTime to -1, so that flow-frames
             * asking for link-credit are not scheduled any more.
             */
            if (linkCredit > 0)
            {
                nextCreditRequestTime = -1;
                incrementedCount = 0;
            }

            if (flow.isSetDrain())
            {
                drainRequested = flow.getDrain();
            }

            if (!sendInProgress)
            {
                if (canSendMessage())
                {
                    sendInProgress = true;
                    messagesParked = true;
                }
                else if (drainRequested)
                {
                    outgoingFlow = processDrainRequested();
                }
            }

            if (flow.isSetEcho() && flow.getEcho() && (outgoingFlow == null))
            {
                outgoingFlow = populateFlowFrame();
            }
        }

        if (outgoingFlow != null)
        {
            session.sendFlow(outgoingFlow);
        }

        if (messagesParked)
        {
            CAMQPSessionManager.getExecutor().execute(this);
        }
    }

    /**
     * Called by {@link CAMQPLinkSendFlowScheduler} to send
     * a flow frame to the link receiver asking for link credit
     * if the link credit < 0 and it has outstanding messages to
     * send.
     *
     * @param currentTime
     */
    void requestCreditIfUnderFlowControl(long currentTime)
    {
        CAMQPControlFlow outgoingFlow = null;
        synchronized (this)
        {
            if ((linkCredit > 0) || (unsentMessages.isEmpty()))
            {
                /*
                 * Is not under flow-control anymore, or doesn't
                 * need link-credit. Reset nextCreditRequestTime.
                 */
                nextCreditRequestTime = -1;
                incrementedCount = 0;
                return;
            }

            /*
             * A flow-control condition was detected.
             * Schedule the sending of flow-frame to until
             * after 2 seconds.
             */
            if (nextCreditRequestTime == -1)
            {
                incrementedCount = 1;
                nextCreditRequestTime = currentTime + 2000*incrementedCount;
                return;
            }

            /*
             * Next credit request time is still in the future,
             * so just return.
             */
            if (currentTime < nextCreditRequestTime)
            {
                return;
            }

            /*
             * Create a flow-frame with current state.
             */
            outgoingFlow = populateFlowFrame();

            /*
             * The subsequent flow-frame sending is scheduled
             * with an exponential back-off (in seconds) :
             * 2, 4, 8, 16, 32
             *
             * After that it is scheduled every 60 seconds.
             */
            if (incrementedCount < 16)
            {
                incrementedCount *= 2;
                nextCreditRequestTime += 2000*incrementedCount;
            }
            else
            {
                nextCreditRequestTime += 60000;
            }
        }

        session.sendFlow(outgoingFlow);
    }

    /**
     * Called by Link source to send a message.
     */
    @Override
    public void sendMessage(CAMQPMessage message)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            if (available >= maxAvailableLimit)
            {
                log.warn("Reached available limit threshold: " + maxAvailableLimit);
                throw new CAMQPLinkSenderFlowControlException("Reached available limit threshold: " + maxAvailableLimit);
            }
            available++;

            if (sendInProgress || (linkCredit <= unsentMessagesAtSession))
            {
                /*
                 * TODO: send flow??
                 */
                unsentMessages.add(message);
                return;
            }

            sendInProgress = true;
            unsentMessagesAtSession++;
        }

        send(message, source);

        boolean parkedMessages = false;
        synchronized (this)
        {
            if (canSendMessage())
            {
                parkedMessages = true;
            }
            else
            {
                sendInProgress = false;
                if (drainRequested)
                {
                    flow = processDrainRequested();
                }
            }
        }

        if (flow != null)
        {
            session.sendFlow(flow);
        }

        if (parkedMessages)
        {
            CAMQPSessionManager.getExecutor().execute(this);
        }
    }

    /**
     * Called by the AMQP session layer after a transfer frame is sent.
     */
    @Override
    public void messageSent(CAMQPControlTransfer transferFrame)
    {
        /*
         * In case of message fragmentation, change the I/O flow
         * state and make link-layer flow-control calculations
         * only when the last fragment has been sent.
         */
        if (transferFrame.getMore())
        {
            return;
        }

        boolean parkedMessages = false;
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            unsentMessagesAtSession--;
            /*
             * Update flow-control attributes
             */
            available--;
            deliveryCount++;
            linkCredit--;

            if (!sendInProgress)
            {
                if (canSendMessage())
                {
                    /*
                     * Messages are waiting at the Link layer
                     * to be sent, throttled by a session-level
                     * flow-control. Send those messages now that
                     * we have enough link credit.
                     */
                    sendInProgress = true;
                    parkedMessages = true;
                }
                else if (drainRequested)
                {
                    flow = processDrainRequested();
                }
            }

            /*
             * Send a flow-frame asking the AMQP peer for more link credit.
             */
            if ((flow == null) && (linkCredit <= 0) && (available > 0))
            {
                flow = populateFlowFrame();
                flow.setEcho(true);
            }
        }

        if (flow != null)
        {
            session.sendFlow(flow);
        }

        if (parkedMessages)
        {
            CAMQPSessionManager.getExecutor().execute(this);
        }
    }

    /**
     * This Runnable runs as a Link sender, and sends all the unsent
     * messages, until it runs out of Link credit.
     *
     * If it is done sending messages, and if there's an outstanding
     * drain requested, it sends a flow frame.
     */
    @Override
    public void run()
    {
        CAMQPMessage message = null;
        CAMQPControlFlow flow = null;
        while (true)
        {
            synchronized (this)
            {
                if (canSendMessage())
                {
                    message = unsentMessages.poll();
                    unsentMessagesAtSession++;
                }
                else
                {
                    sendInProgress = false;
                    if (drainRequested)
                    {
                        flow = processDrainRequested();
                    }
                    break;
                }
            }

            send(message, source);
        }

        if (flow != null)
        {
            session.sendFlow(flow);
        }
    }

    @GuardedBy("this")
    private CAMQPControlFlow processDrainRequested()
    {
        drainRequested = false;
        if (hasLinkCreditButNoMessage())
        {
            deliveryCount += linkCredit;
            linkCredit = 0;
            return populateFlowFrame();
        }
        else
        {
            return null;
        }
    }
    /**
     * Returns true if there are outstanding messages to be sent, and there is enough
     * credit. If there are outstanding unsent messages at the session layer, the Link
     * layer acts as if it does not have the link credit.
     * @return
     */
    @GuardedBy("this")
    private boolean canSendMessage()
    {
        return ((linkCredit > unsentMessagesAtSession) && (!unsentMessages.isEmpty()));
    }

    @GuardedBy("this")
    private boolean hasLinkCreditButNoMessage()
    {
        return (unsentMessages.isEmpty() && (linkCredit > 0));
    }

    @Override
    public LinkRole getRole()
    {
        return LinkRole.LinkSender;
    }

    @Override
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
    {
        if (source != null)
        {
            return source.processDisposition(deliveryIds, isMessageSettledByPeer, newState);
        }
        return deliveryIds;
    }

    @Override
    Object getEndpoint()
    {
        return source;
    }

    @Override
    public long getHandle()
    {
        return linkHandle;
    }

    @Override
    public void attached(boolean isInitiator)
    {
        super.attached(isInitiator);
        registerWithFlowScheduler();
    }

    @Override
    public void detached(boolean isInitiator)
    {
        unregisterWithFlowScheduler();
        super.detached(isInitiator);
    }

    void registerWithFlowScheduler()
    {
        CAMQPLinkManager.getLinkmanager().getFlowScheduler().registerLinkSender(linkHandle, this);
    }

    void unregisterWithFlowScheduler()
    {
        CAMQPLinkManager.getLinkmanager().getFlowScheduler().unregisterLinkSender(linkHandle);
    }

    /**
     * Establishes an AMQP link to a remote AMQP end-point.
     * Then waits for the first non-zero link-credit.
     * @param sourceName
     * @param targetName
     */
    @Override
    void createLink(String sourceName, String targetName, CAMQPEndpointPolicy endpointPolicy)
    {
        super.createLink(sourceName, targetName, endpointPolicy);
        synchronized (this)
        {
            while (!receivedFirstFlowCredit || (linkCredit <= 0))
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
