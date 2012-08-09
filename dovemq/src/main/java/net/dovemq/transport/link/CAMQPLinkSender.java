package net.dovemq.transport.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import net.jcip.annotations.GuardedBy;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

class CAMQPLinkSender extends CAMQPLinkEndpoint implements CAMQPLinkSenderInterface, Runnable
{
    private static final Logger log = Logger.getLogger(CAMQPLinkSender.class);
    
    private final CAMQPSessionInterface session;
    
    private final Map<String, CAMQPMessagePayload> unsettledDeliveries = new ConcurrentHashMap<String, CAMQPMessagePayload>();
    
    private final ConcurrentLinkedQueue<CAMQPMessage> unsentMessages = new ConcurrentLinkedQueue<CAMQPMessage>();
    
    /*
     * Used to keep track of how many unsent messages are outstanding at session for this link.
     * A non-zero value is an indication that session is under flow-control.
     * 
     * Incremented before call to CAMQPSessionInterface.sendTransfer()
     * Decremented in the callback from session layer: messageSent()
     * 
     * Flow-control calculation in the link layer take this value into account.
     * If linkCredit <= unsentMessagesAtSession, the link layer is under flow-control.
     *
     */
    private long unsentMessagesAtSession = 0;

    private boolean sendInProgress = false;
    private boolean drainRequested = false;
    
    private long maxAvailableLimit;
    
    void setMaxAvailableLimit(long maxAvailableLimit)
    {
        this.maxAvailableLimit = maxAvailableLimit;
    }

    public CAMQPLinkSender(CAMQPSessionInterface session)
    {
        super(CAMQPLinkConstants.ROLE_SENDER);
        this.session = session;
        maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
    }
    
    @Override
    public CAMQPSessionInterface getSession()
    {
        return session;
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        // REVISIT TODO error condition : should never be called for CAMQPLinkSender
    }

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
                    linkCredit = flow.getLinkCredit() - (deliveryCount - flow.getDeliveryCount());
                else
                    linkCredit = flow.getLinkCredit() - deliveryCount;
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
                    outgoingFlow = processDrainRequested();
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
    public void sendMessage(String deliveryTag, CAMQPMessagePayload message)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            if (available >= maxAvailableLimit)
            {
                /*
                 * REVISIT TODO throw exception to throttle the Source
                 */
                log.warn("Reached available limit threshold: " + maxAvailableLimit);
                return;
            }
            available++;
            unsettledDeliveries.put(deliveryTag, message);
            
            if (sendInProgress || (linkCredit <= unsentMessagesAtSession))
            {
                /*
                 * REVISIT TODO: send flow??
                 */
                unsentMessages.add(new CAMQPMessage(deliveryTag, message));
                return;
            }
            
            sendInProgress = true;
            unsentMessagesAtSession++;
        }
    
        send(deliveryTag, message);

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
            session.sendFlow(flow);
        
        if (parkedMessages)
        {
            CAMQPSessionManager.getExecutor().execute(this);
        }
    }

    @Override
    public void attached()
    {       
    }

    @Override
    public void detached()
    {   
    }

    @Override
    public void messageSent(CAMQPControlTransfer transferFrame)
    {
        /*
         * In case of message fragmentation, change the I/O flow
         * state only when the last fragment has been sent
         */
        if (transferFrame.getMore())
            return;
        
        boolean parkedMessages = false;
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            //messagesOutstandingAtSession.remove(deliveryTag);
            unsentMessagesAtSession--;
            
            available--;
            deliveryCount++;
            linkCredit--;
            
            if (!sendInProgress)
            {
                if (canSendMessage())
                {
                    sendInProgress = true;
                    parkedMessages = true;
                }
                else if (drainRequested)
                    flow = processDrainRequested();
            }
            
            if ((flow == null) && (linkCredit == 0) && (available > 0))
            {
                flow = populateFlowFrame();
                flow.setEcho(true);
            }
        }
        
        if (flow != null)
            session.sendFlow(flow);
        
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
        while (true)
        {
            synchronized (this)
            {
                if (canSendMessage())
                {
                    message = unsentMessages.poll();
                    //messagesOutstandingAtSession.add(message.getDeliveryTag());
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
            
            send(message.getDeliveryTag(), message.getPayload());
        }
        
        if (flow != null)
            session.sendFlow(flow);
    }
    
    private void send(String deliveryTag, CAMQPMessagePayload message)
    {
        long deliveryId = session.getNextDeliveryId();
        CAMQPControlTransfer transferFrame = new CAMQPControlTransfer();
        transferFrame.setDeliveryId(deliveryId);
        transferFrame.setMore(false);
        transferFrame.setHandle(linkHandle);
        transferFrame.setDeliveryTag(deliveryTag.getBytes());
 
        session.sendTransfer(transferFrame, message, this);
    }
    
    @GuardedBy("this")
    private CAMQPControlFlow processDrainRequested()
    {
        drainRequested = false;
        if (enoughCreditButNoMessages())
        {
            deliveryCount += linkCredit;
            linkCredit = 0;
            return populateFlowFrame();
        }
        else
            return null;
    }
    
    @GuardedBy("this")
    private boolean canSendMessage()
    {
        //return ((linkCredit > 0) && (!unsentMessages.isEmpty()));
        
        return ((linkCredit > unsentMessagesAtSession) && (!unsentMessages.isEmpty()));
    }
    
    @GuardedBy("this")
    private boolean enoughCreditButNoMessages()
    {
        return (unsentMessages.isEmpty() && (linkCredit > 0));
    }
}
