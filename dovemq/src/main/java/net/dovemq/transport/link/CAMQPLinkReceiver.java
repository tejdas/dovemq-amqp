package net.dovemq.transport.link;

import java.util.Collection;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.ReceiverLinkCreditPolicy;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;

import org.apache.log4j.Logger;

/**
 * Implementation of AMQP Link Receiver.
 * @author tejdas
 */
public class CAMQPLinkReceiver extends CAMQPLinkEndpoint implements CAMQPLinkReceiverInterface
{
    private static final Logger log = Logger.getLogger(CAMQPLinkReceiver.class);

    private long timeLastFlowFrameSent = System.currentTimeMillis();
    private long messagesProcessedSinceLastSendFlow = 0;
    private long targetIssuedLinkCredit = -1;
    private CAMQPTargetInterface target = null;
    public void setTarget(CAMQPTargetInterface target)
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

    public CAMQPLinkReceiver(CAMQPSessionInterface session)
    {
        super(session);
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

            if ((linkCredit < 0) && (targetIssuedLinkCredit > 0))
            {
                linkCredit = targetIssuedLinkCredit;
                targetIssuedLinkCredit = -1;
            }

            if ((linkCredit < 0) && (-linkCredit > CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT))
            {
                /*
                 * Link sender violated the link credit limit. Destroy the link
                 * with error code: LINK_ERROR_TRANSFER_LIMIT_EXCEEDED
                 */
                violatedLinkCredit = true;
                System.out.println("violated link credit: closing link. Link credit should not have gone below -" + CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT + " but is now " + linkCredit);
                log.fatal("violated link credit: closing link. Link credit should not have gone below -" + CAMQPLinkConstants.LINK_CREDIT_VIOLATION_LIMIT + " but is now " + linkCredit);
            }
            else
            {
                /*
                 * To keep the flow at steady state, automatically boost the link credit
                 * and send a flow-frame to the Link sender, if the link credit drops
                 * below minLinkCreditThreshold.
                 */
                if (linkCredit < minLinkCreditThreshold)
                {
                    if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE)
                    {
                        linkCredit = minLinkCreditThreshold + linkCreditBoost;
                        flow = populateFlowFrame();
                    }
                    else if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING)
                    {
                        System.out.println("reason3: " + messagesProcessedSinceLastSendFlow);
                        if (messagesProcessedSinceLastSendFlow >= (linkCreditBoost - minLinkCreditThreshold))
                        {
                            linkCredit += messagesProcessedSinceLastSendFlow;
                            messagesProcessedSinceLastSendFlow = 0;
                            timeLastFlowFrameSent = System.currentTimeMillis();
                            flow = populateFlowFrame();
                        }
                    }
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
        target.messageReceived(transferFrame.getDeliveryId(), deliveryTag, payload, transferFrame.getSettled(), transferFrame.getRcvSettleMode());
    }

    /**
     * Processes an incoming Link flow-frame:
     *   (a) updates flow-control attributes.
     *   (b) Gives linkCredit to sender based on policy and flow frame.
     *   (c) Sends back a flow frame if needed.
     */
    @Override
    public void flowReceived(CAMQPControlFlow inFlow)
    {
        CAMQPControlFlow outFlow = null;
        synchronized (this)
        {
            if (inFlow.isSetAvailable())
            {
                available = inFlow.getAvailable();
            }

            if (inFlow.isSetEcho() && inFlow.getEcho())
            {
                outFlow = populateFlowFrame();
            }

            if (inFlow.isSetDeliveryCount())
            {
                deliveryCount = inFlow.getDeliveryCount();
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
                else if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING)
                {
                    if (messagesProcessedSinceLastSendFlow > 0)
                    {
                        linkCredit = messagesProcessedSinceLastSendFlow;
                        messagesProcessedSinceLastSendFlow = 0;
                        timeLastFlowFrameSent = System.currentTimeMillis();
                        outFlow = populateFlowFrame();
                    }
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

    /**
     * Called by target to alter linkCredit of receiver.
     * Note that the linkCredit is not changed right away.
     * The reason is that the Link sender might be sending
     * messages with the last known link credit, and if we
     * suddenly reduce the linkCredit of the receiver, the
     * sender might appear to overrun the link credit, if it
     * does not get the new flow frame until it has sent
     * enough messages to appear to have breached the link credit.
     *
     * So, we store the newLinkCredit in a member attribute
     * targetIssuedLinkCredit. The linkCredit is altered only
     * after it has been exhausted. In other words, the sender
     * is provided with a new link credit only after it has used
     * up its current linkCredit.
     *
     * @param newLinkCredit
     * @param drain
     */
    private void modifyLinkCredit(long newLinkCredit, boolean drain)
    {
        if (newLinkCredit <= 0)
            return;
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_OFFERED_BY_TARGET;
            targetIssuedLinkCredit = newLinkCredit;
            if (linkCredit <= 0)
            {
                linkCredit = targetIssuedLinkCredit;
                targetIssuedLinkCredit = -1;
            }
            flow = populateFlowFrame();
            flow.setDrain(drain);
        }

        session.sendFlow(flow);
    }

    @Override
    public void issueLinkCredit(long linkCreditBoost)
    {
        modifyLinkCredit(linkCreditBoost, false);
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
        modifyLinkCredit(messageCount, true);
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
    public LinkRole getRole()
    {
        return LinkRole.LinkReceiver;
    }

    @Override
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
    {
        if (target != null)
        {
            return target.processDisposition(deliveryIds, isMessageSettledByPeer, newState);
        }
        return deliveryIds;
    }

    @Override
    public void attached(boolean isInitiator)
    {
        super.attached(isInitiator);
        synchronized (this)
        {
            linkCreditPolicy = endpointPolicy.getLinkCreditPolicy();
            minLinkCreditThreshold = endpointPolicy.getMinLinkCreditThreshold();
            linkCreditBoost = endpointPolicy.getLinkCreditBoost();
        }
    }

    @Override
    public void acnowledgeMessageProcessingComplete()
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            long timeout = linkCreditBoost * 20;
            if (linkCreditPolicy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING)
            {
                messagesProcessedSinceLastSendFlow++;

                if ((System.currentTimeMillis() - timeLastFlowFrameSent) >= timeout)
                {
                    timeLastFlowFrameSent = System.currentTimeMillis();
                    linkCredit += messagesProcessedSinceLastSendFlow;
                    messagesProcessedSinceLastSendFlow = 0;
                    flow = populateFlowFrame();
                    System.out.println("reason1: " + linkCredit);
                }
                else if (messagesProcessedSinceLastSendFlow >= (linkCreditBoost - minLinkCreditThreshold))
                {
                    linkCredit += messagesProcessedSinceLastSendFlow;
                    messagesProcessedSinceLastSendFlow = 0;
                    flow = populateFlowFrame();
                    timeLastFlowFrameSent = System.currentTimeMillis();
                    System.out.println("reason2: " + linkCredit);
                }
            }
        }
        if (flow != null)
        {
            session.sendFlow(flow);
        }
    }

    @Override
    public void configureSteadyStatePacedByMessageProcessing(long minLinkCreditThreshold,
            long linkCreditBoost)
    {
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            this.minLinkCreditThreshold = minLinkCreditThreshold;
            this.linkCreditBoost = linkCreditBoost;
            linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING;
            linkCredit = linkCreditBoost;
            flow = populateFlowFrame();
        }

        session.sendFlow(flow);
    }
}
