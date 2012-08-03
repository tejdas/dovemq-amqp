package net.dovemq.transport.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.GuardedBy;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

class CAMQPLinkAsyncSender extends CAMQPLinkEndpoint implements CAMQPLinkSenderInterface, Runnable
{
    private final CAMQPSessionInterface session;
    private CAMQPSourceInterface source = null;
    
    void setSource(CAMQPSourceInterface source)
    {
        this.source = source;
    }

    private final Map<String, CAMQPMessagePayload> unsettledDeliveries = new ConcurrentHashMap<String, CAMQPMessagePayload>();
    
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
        super(CAMQPLinkConstants.ROLE_SENDER);
        this.session = session;
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
        
        long availableMessageCount = source.getMessageCount();
        synchronized (this)
        {
            available = availableMessageCount;
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
    public void sendMessage(String deliveryTag, CAMQPMessagePayload payload)
    {
        /*
         * Not implemented
         */
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
        //System.out.println("messageSent: " + transferFrame.getDeliveryId());
        /*
         * In case of message fragmentation, change the I/O flow
         * state only when the last fragment has been sent
         */
        if (transferFrame.getMore())
            return;
        
        boolean checkMessageAvailability = false;
        boolean parkedMessages = false;
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            messageOutstanding.set(false);
            
            if (available > 0)
                available--;
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
                available = messageCount;

            if (!sendInProgress)
            {
                if ((available > 0) && (linkCredit > 0))
                {
                    parkedMessages = true;
                    sendInProgress = true;
                }
                else if (drainRequested)
                    flow = processDrainRequested(true);
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
        String deliveryTag = null;
        
        while (true)
        {
            message = source.getMessage();
            if (message != null)
            {
                deliveryTag = message.getDeliveryTag();
                unsettledDeliveries.put(deliveryTag, message.getPayload());
                messageOutstanding.set(true);
                send(deliveryTag, message.getPayload());
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
    private CAMQPControlFlow processDrainRequested(boolean availableKnown)
    {
        if (linkCredit > 0)
            deliveryCount += linkCredit;
        linkCredit = 0;
        drainRequested = false;
        if (availableKnown)
            return populateFlowFrame();
        else
            return populateFlowFrameAvailableUnknown();
    }
}
