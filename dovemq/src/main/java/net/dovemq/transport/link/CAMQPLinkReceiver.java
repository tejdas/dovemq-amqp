package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;

enum ReceiverLinkCreditPolicy
{
    CREDIT_OFFERED_BY_TARGET,
    CREDIT_STEADY_STATE,
    CREDIT_AS_DEMANDED_BY_SENDER
}

class CAMQPLinkReceiver extends CAMQPLinkEndpoint implements CAMQPLinkReceiverInterface
{
    private final CAMQPSessionInterface session;
    private CAMQPTargetInterface target = null;
    void setTarget(CAMQPTargetInterface target)
    {
        this.target = target;
    }

    /*
     * TODO : How do we configure steady state?
     */
    private ReceiverLinkCreditPolicy linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET;
    private long minLinkCreditThreshold = 0;
    private long linkCreditBoost = 0;

    public CAMQPLinkReceiver(CAMQPSessionInterface session)
    {
        super(CAMQPLinkConstants.ROLE_RECEIVER);
        this.session = session;
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        CAMQPControlFlow flow = null;
        boolean violatedLinkCredit = false;
        synchronized (this)
        {
            if (available > 0)
                available--;
            deliveryCount++;
            linkCredit--;
            
            if ((linkCredit < 0) && (-linkCredit > CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT))
            {
                violatedLinkCredit = true;
            }
            else
            {
                if ((linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE) && (linkCredit < minLinkCreditThreshold))
                {
                    linkCredit = minLinkCreditThreshold + linkCreditBoost;
                    flow = populateFlowFrame();
                }
            }
        }
        
        if (violatedLinkCredit)
        {
            destroyLink(CAMQPConstants.LINK_ERROR_TRANSFER_LIMIT_EXCEEDED);
            return;
        }
        
        if (flow != null)
            session.sendFlow(flow);
                
        deliverMessage(transferFrame, payload);
        session.ackTransfer(transferId);
    }
    
    private void deliverMessage(CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        String deliveryTag = new String(transferFrame.getDeliveryTag());
        target.messageReceived(deliveryTag, payload);
    }
   
    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        CAMQPControlFlow outFlow = null;
        synchronized (this)
        {
            if (flow.isSetAvailable())
                available = flow.getAvailable();
            
            if (flow.isSetEcho() && flow.getEcho())
            {
                outFlow = populateFlowFrame();
            }
            
            if (flow.isSetDeliveryCount())
                deliveryCount = flow.getDeliveryCount();
            
            if ((available > 0) && (linkCredit <= 0))
            {
                if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE)
                {
                    linkCredit = minLinkCreditThreshold + linkCreditBoost;
                    outFlow = populateFlowFrame();
                }
                else if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_AS_DEMANDED_BY_SENDER)
                    linkCredit = available;
            }
        }
        
        if (outFlow != null)
            session.sendFlow(outFlow);
    }

    @Override
    public void sessionClosed()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attached()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void detached()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public CAMQPSessionInterface getSession()
    {
        // TODO Auto-generated method stub
        return session;
    }

    @Override
    public void getMessages(int messageCount)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET;
            linkCredit = messageCount;
            flow = populateFlowFrame();
        }
        
        session.sendFlow(flow);
    }

    @Override
    public void flowMessages(long minLinkCreditThreshold, long linkCreditBoost)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            this.minLinkCreditThreshold = minLinkCreditThreshold;
            this.linkCreditBoost = linkCreditBoost;
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
            
            if (linkCredit < this.minLinkCreditThreshold)
            {
                linkCredit = this.minLinkCreditThreshold + this.linkCreditBoost;
                flow = populateFlowFrame();
            }
        }
        
        if (flow != null)
            session.sendFlow(flow);
    }

    @Override
    public void stop()
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET;
            if (linkCredit > 0)
            {
                linkCredit = 0;
                flow = populateFlowFrame();
            }
        }
        
        if (flow != null)
            session.sendFlow(flow);
    }
}
