package net.dovemq.transport.endpoint;

import net.dovemq.transport.link.CAMQPLinkConstants;
import net.dovemq.transport.protocol.data.CAMQPConstants;

public final class CAMQPEndpointPolicy
{
    public static enum CAMQPMessageDeliveryPolicy
    {
        AtleastOnce,
        AtmostOnce,
        ExactlyOnce
    }
    
    /**
     * ReceiverLinkCreditPolicy determines how the Link credit
     * is increased when it drops to (below) zero.
     * 
     * @author tejdas
     */
    public static enum ReceiverLinkCreditPolicy
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

    private final long maxMessageSize;
    private final long maxAvailableLimit;
    private final int senderSettleMode;
    private final int receiverSettleMode;
    private final CAMQPMessageDeliveryPolicy deliveryPolicy;
    private final ReceiverLinkCreditPolicy linkCreditPolicy;
    private final long minLinkCreditThreshold;
    private final long linkCreditBoost;
    
    public CAMQPMessageDeliveryPolicy getDeliveryPolicy()
    {
        return deliveryPolicy;
    }
    public long getMaxMessageSize()
    {
        return maxMessageSize;
    }
    public long getMaxAvailableLimit()
    {
        return maxAvailableLimit;
    }
    public int getSenderSettleMode()
    {
        return senderSettleMode;
    }
    public int getReceiverSettleMode()
    {
        return receiverSettleMode;
    }
    public ReceiverLinkCreditPolicy getLinkCreditPolicy()
    {
        return linkCreditPolicy;
    }
    public long getMinLinkCreditThreshold()
    {
        return minLinkCreditThreshold;
    }
    public long getLinkCreditBoost()
    {
        return linkCreditBoost;
    }

    public CAMQPEndpointPolicy(long maxMessageSize,
            long maxAvailableLimit,
            String senderSettleMode,
            String receiverSettleMode,
            CAMQPMessageDeliveryPolicy deliveryPolicy,
            ReceiverLinkCreditPolicy linkCreditPolicy,
            long minLinkCreditThreshold,
            long linkCreditBoost)
    {
        super();
        this.maxMessageSize = maxMessageSize;
        this.maxAvailableLimit = maxAvailableLimit;
        this.deliveryPolicy = deliveryPolicy;
        this.linkCreditPolicy = linkCreditPolicy;
        this.minLinkCreditThreshold = minLinkCreditThreshold;
        this.linkCreditBoost = linkCreditBoost;
        

        if (senderSettleMode.equalsIgnoreCase(CAMQPConstants.SENDER_SETTLE_MODE_SETTLED_STR))
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_SETTLED;
        else if (senderSettleMode.equalsIgnoreCase(CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED_STR))
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
        else
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED;
        
        if (receiverSettleMode.equalsIgnoreCase(CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST_STR))
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
        else
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
    }
    
    public CAMQPEndpointPolicy(long maxMessageSize,
            int senderSettleMode,
            int receiverSettleMode,
            CAMQPEndpointPolicy that)
    {
        super();
        this.maxMessageSize = maxMessageSize;
        this.maxAvailableLimit = that.maxAvailableLimit;
        this.senderSettleMode = senderSettleMode;
        this.receiverSettleMode = receiverSettleMode;
        linkCreditPolicy = that.linkCreditPolicy;
        minLinkCreditThreshold = that.minLinkCreditThreshold;
        linkCreditBoost = that.linkCreditBoost;
        
        if ((senderSettleMode == CAMQPConstants.SENDER_SETTLE_MODE_SETTLED) &&
            (receiverSettleMode == CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST))
        {
            deliveryPolicy = CAMQPMessageDeliveryPolicy.AtmostOnce;
        }
        else if ((senderSettleMode == CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED) &&
                 (receiverSettleMode == CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST))
        {
            deliveryPolicy = CAMQPMessageDeliveryPolicy.AtleastOnce;
        }
        else
        {
            deliveryPolicy = that.deliveryPolicy;
        }
    }

    public CAMQPEndpointPolicy(CAMQPMessageDeliveryPolicy deliveryPolicy)
    {
        super();
        this.maxMessageSize = CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE;
        this.maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
        linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
        minLinkCreditThreshold = 10;
        linkCreditBoost = 100;
        this.deliveryPolicy = deliveryPolicy;
        switch (deliveryPolicy)
        {
        case AtmostOnce:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_SETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
            break;
            
        case AtleastOnce:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
            break;
            
        case ExactlyOnce:
        default:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
            break;
        }
    }
    
    public CAMQPEndpointPolicy()
    {
        super();
        this.maxMessageSize = CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE;
        this.maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
        this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED;
        this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
        deliveryPolicy = CAMQPMessageDeliveryPolicy.ExactlyOnce;
        linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
        minLinkCreditThreshold = 10;
        linkCreditBoost = 100;
    } 
}
