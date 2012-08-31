package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;

/**
 * ReceiverLinkCreditPolicy determines how the Link credit
 * is increased when it drops to (below) zero.
 * 
 * @author tejdas
 */
enum ReceiverLinkCreditPolicy
{
    /*
     * Link credit is offered by the target receiver, whenever
     * it wants to get message(s).
     */
    CREDIT_OFFERED_BY_TARGET,
    /*
     * When the Link receiver's computed link credit goes below
     * a certain threshold, it automatically increases the link
     * credit. This enables messages to flow at a steady state,
     * and the Link sender never runs out of link-credit.
     */
    CREDIT_STEADY_STATE,
    /*
     * Link credit is incremented whenever Link sender has no
     * more credit, and sends a flow-frame to ask for more credit.
     */
    CREDIT_AS_DEMANDED_BY_SENDER
}

/**
 * Implementation of AMQP Link Receiver.
 * @author tejdas
 */
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
    void setLinkCreditPolicy(ReceiverLinkCreditPolicy linkCreditPolicy)
    {
        this.linkCreditPolicy = linkCreditPolicy;
    }

    private long minLinkCreditThreshold = 0;
    private long linkCreditBoost = 0;

    synchronized void setLinkCreditSteadyState(long linkCreditBoost, long minLinkCreditThreshold)
    {
        this.linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
        this.linkCreditBoost = linkCreditBoost;
        this.minLinkCreditThreshold = minLinkCreditThreshold;
    }

    public CAMQPLinkReceiver(CAMQPSessionInterface session)
    {
        super();
        this.session = session;
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        /*
         * In case of message fragmentation, change the I/O flow
         * state and make link-layer flow-control calculations
         * only when the last fragment has been received.
         */
        if (transferFrame.getMore())
        {
            return;
        }
 
        CAMQPControlFlow flow = null;
        boolean violatedLinkCredit = false;
        synchronized (this)
        {
            /*
             * update flow-control attributes
             */
            if (available > 0)
            {
                available--;
            }
            deliveryCount++;
            linkCredit--;
            
            if ((linkCredit < 0) && (-linkCredit > CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT))
            {
                /*
                 * Link sender violated the link credit limit. Destroy the link
                 * with error code: LINK_ERROR_TRANSFER_LIMIT_EXCEEDED
                 */
                violatedLinkCredit = true;
            }
            else
            {
                /*
                 * To keep the flow at steady state, automatically boost the link credit
                 * and send a flow-frame to the Link sender, if the link credit drops
                 * below minLinkCreditThreshold.
                 */
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
        {
            session.sendFlow(flow);
        }
 
        /*
         * Deliver the message to the Link target.
         * TODO reassemble transfer frames of a
         * fragmented message.
         */
        deliverMessage(transferFrame, payload);
        /*
         * Acknowledge the receipt of transfer frame to
         * the Session layer.
         */
        session.ackTransfer(transferId);
    }

    /**
     * Deliver the message to Link target.
     * @param transferFrame
     * @param payload
     */
    private void deliverMessage(CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        String deliveryTag = new String(transferFrame.getDeliveryTag());
        target.messageReceived(deliveryTag, payload);
    }
 
    /**
     * Processes an incoming Link flow-frame:
     *   (a) updates flow-control attributes.
     *   (b) Gives linkCredit to sender based on policy and flow frame.
     *   (c) Sends back a flow frame if needed.
     */
    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        CAMQPControlFlow outFlow = null;
        synchronized (this)
        {
            if (flow.isSetAvailable())
            {
                available = flow.getAvailable();
            }
            
            if (flow.isSetEcho() && flow.getEcho())
            {
                outFlow = populateFlowFrame();
            }
            
            if (flow.isSetDeliveryCount())
            {
                deliveryCount = flow.getDeliveryCount();
            }
            
            /*
             * Link sender has messages available, but has
             * run out of link credit. Issue link credit
             * based on the policy.
             */
            if ((available > 0) && (linkCredit <= 0))
            {
                if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE)
                {
                    linkCredit = minLinkCreditThreshold + linkCreditBoost;
                    outFlow = populateFlowFrame();
                }
                else if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_AS_DEMANDED_BY_SENDER)
                {
                    linkCredit = available;
                    outFlow = populateFlowFrame();
                }
            }
        }
        
        if (outFlow != null)
        {
            session.sendFlow(outFlow);
        }
    }

    @Override
    public void sessionClosed()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public CAMQPSessionInterface getSession()
    {
        // TODO Auto-generated method stub
        return session;
    }
    
    private void boostLinkCredit(long linkCreditBoost, boolean drain)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET;
            linkCredit = linkCreditBoost;
            flow = populateFlowFrame();
            flow.setDrain(drain);
        }
        
        session.sendFlow(flow);
    }

    
    @Override
    public void issueLinkCredit(long linkCreditBoost)
    {
        boostLinkCredit(linkCreditBoost, false);
    }    

    /**
     * Called by Link target to asynchronously receive
     * messages. Offers a link credit that is equal
     * to the expected message count. Also sets the drain
     * flag to true, so that the Link sender advances
     * deliveryCount, even if enough messages are not
     * available.
     */
    @Override
    public void getMessages(int messageCount)
    {
        boostLinkCredit(messageCount, true);
    }

    /**
     * Called by Link target to configure the steady-state link credit
     * policy. It also immediately boosts the link credit if necessary.
     * Subsequently, whenever the link sender's link credit drops below
     * the configurable threshold, the link credit is boosted, and a flow
     * frame generated.
     */
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
        {
            session.sendFlow(flow);
        }
    }

    /**
     * Called by Link target to stop receiving messages on the link,
     * by dropping the link credit to 0. Note that, the LinkReceiver
     * might still receive messages that are on the flight, or are sent
     * by the LinkSender before this flow frame is processed.
     */
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
        {
            session.sendFlow(flow);
        }
    }

    @Override
    LinkRole getRole()
    {
        return LinkRole.LinkReceiver;
    }
}
