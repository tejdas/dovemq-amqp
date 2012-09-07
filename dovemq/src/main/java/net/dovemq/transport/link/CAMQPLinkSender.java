package net.dovemq.transport.link;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import net.jcip.annotations.GuardedBy;

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

/**
 * Implements AMQP Link Sender.
 * @author tejdas
 *
 */
public class CAMQPLinkSender extends CAMQPLinkEndpoint implements CAMQPLinkSenderInterface, Runnable
{
    private static final Logger log = Logger.getLogger(CAMQPLinkSender.class);
    
    private CAMQPSourceInterface source = null;
    
    public void setSource(CAMQPSourceInterface source)
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
     * Set to true if drain is requested by the peer and until it is processed.
     */
    private boolean drainRequested = false;
    
    private long maxAvailableLimit;
    
    public void setMaxAvailableLimit(long maxAvailableLimit)
    {
        this.maxAvailableLimit = maxAvailableLimit;
    }

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

    @Override
    public void sessionClosed()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Called by Link source to send a message.
     */
    @Override
    public void sendMessage(String deliveryTag, CAMQPMessagePayload message)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            if (available >= maxAvailableLimit)
            {
                /*
                 * TODO throw exception to throttle the Source
                 */
                log.warn("Reached available limit threshold: " + maxAvailableLimit);
                return;
            }
            available++;
            
            if (sendInProgress || (linkCredit <= unsentMessagesAtSession))
            {
                /*
                 * TODO: send flow??
                 */
                unsentMessages.add(new CAMQPMessage(deliveryTag, message));
                return;
            }
            
            sendInProgress = true;
            unsentMessagesAtSession++;
        }
    
        send(deliveryTag, message, this, source);

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
            
            send(message.getDeliveryTag(), message.getPayload(), this, source);
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
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds, boolean settleMode, Object newState)
    {
        if (source != null)
        {
            return source.processDisposition(deliveryIds, settleMode, newState);
        }
        return deliveryIds;
    }
}
