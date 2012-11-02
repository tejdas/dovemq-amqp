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
import java.util.concurrent.atomic.AtomicBoolean;

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Asynchronous Link sender implementation.
 * CAMQPLinkAsyncSender picks up message from the Link Source
 * and sends it.
 *
 * @author tejdas
 */
@ThreadSafe
class CAMQPLinkAsyncSender extends CAMQPLinkEndpoint implements CAMQPLinkSenderInterface, Runnable
{
    private volatile CAMQPSourceInterface source = null;

    @Override
    public void registerSource(CAMQPSourceInterface source)
    {
        this.source = source;
    }

    /*
     * messageOutstanding is set to true before calling CAMQPSessionInterface.sendTransfer()
     *
     * It is set to false after the message has been sent by the session layer, in the callback:
     * messageSent()
     *
     */
    private AtomicBoolean messageOutstanding = new AtomicBoolean(false);

    private boolean sendInProgress = false;
    private boolean drainRequested = false;

    public CAMQPLinkAsyncSender(CAMQPSessionInterface session)
    {
        super(session);
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        // TODO error condition : should never be called for CAMQPLinkSender
    }

    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        boolean messagesParked = false;
        CAMQPControlFlow outgoingFlow = null;

        long availableMessageCount = source.getMessageCount();
        synchronized (this)
        {
            available = availableMessageCount;
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

            if (flow.isSetDrain())
            {
                drainRequested = flow.getDrain();
            }

            if (!sendInProgress)
            {
                if ((available > 0) && (linkCredit > 0))
                {
                    sendInProgress = true;
                    messagesParked = true;
                }
                else if (drainRequested)
                {
                    outgoingFlow = processDrainRequested(true);
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

    @Override
    public void sessionClosed()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void sendMessage(CAMQPMessage message)
    {
        /*
         * Not implemented
         */
    }

    @Override
    public void messageSent(CAMQPControlTransfer transferFrame)
    {
        /*
         * In case of message fragmentation, change the I/O flow
         * state only when the last fragment has been sent
         */
        if (transferFrame.getMore())
        {
            return;
        }

        boolean checkMessageAvailability = false;
        boolean parkedMessages = false;
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            messageOutstanding.set(false);

            if (available > 0)
            {
                available--;
            }
            deliveryCount++;
            linkCredit--;

            if (!sendInProgress)
            {
                checkMessageAvailability = ((linkCredit > 0) && (available == 0));

                if (drainRequested && (linkCredit <= 0) && !checkMessageAvailability)
                {
                    flow = processDrainRequested(available > 0);
                }
            }
        }

        long messageCount = 0;
        if (checkMessageAvailability)
        {
            messageCount = source.getMessageCount();
        }

        synchronized (this)
        {
            if (checkMessageAvailability)
            {
                available = messageCount;
            }

            if (!sendInProgress)
            {
                if ((available > 0) && (linkCredit > 0))
                {
                    parkedMessages = true;
                    sendInProgress = true;
                }
                else if (drainRequested)
                {
                    flow = processDrainRequested(true);
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

    @Override
    public void run()
    {
        CAMQPMessage message = null;
        CAMQPControlFlow flow = null;
        String deliveryTag = null;

        while (true)
        {
            message = source.getMessage();
            if (message != null)
            {
                deliveryTag = message.getDeliveryTag();
                messageOutstanding.set(true);
                send(message, source);
            }

            synchronized (this)
            {
                if ((deliveryTag != null) && messageOutstanding.get())
                {
                    sendInProgress = false;
                    break;
                }

                if ((message == null) || (linkCredit <= 0))
                {
                    sendInProgress = false;

                    if (drainRequested)
                    {
                        flow = processDrainRequested(available > 0);
                    }
                    break;
                }
            }
        }

        if (flow != null)
        {
            session.sendFlow(flow);
        }
    }

    @GuardedBy("this")
    private CAMQPControlFlow processDrainRequested(boolean availableKnown)
    {
        if (linkCredit > 0)
            deliveryCount += linkCredit;
        linkCredit = 0;
        drainRequested = false;
        if (availableKnown)
        {
            return populateFlowFrame();
        }
        else
        {
            return populateFlowFrameAvailableUnknown();
        }
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
}
