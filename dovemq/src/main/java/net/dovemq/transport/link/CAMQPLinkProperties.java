package net.dovemq.transport.link;

import net.dovemq.transport.protocol.data.CAMQPConstants;

enum MessageDeliveryPolicy
{
    AtleastOnce,
    AtmostOnce,
    ExactlyOnce
}

public class CAMQPLinkProperties
{
    private final long maxMessageSize;
    private final String senderSettleMode;
    private final String receiverSettleMode;
    private final MessageDeliveryPolicy deliveryPolicy;
    
    public MessageDeliveryPolicy getDeliveryPolicy()
    {
        return deliveryPolicy;
    }
    public long getMaxMessageSize()
    {
        return maxMessageSize;
    }
    public String getSenderSettleMode()
    {
        return senderSettleMode;
    }
    public String getReceiverSettleMode()
    {
        return receiverSettleMode;
    }
    public CAMQPLinkProperties(long maxMessageSize,
            String senderSettleMode,
            String receiverSettleMode,
            MessageDeliveryPolicy deliveryPolicy)
    {
        super();
        this.maxMessageSize = maxMessageSize;
        this.senderSettleMode = senderSettleMode;
        this.receiverSettleMode = receiverSettleMode;
        this.deliveryPolicy = deliveryPolicy;
    }
    
    public CAMQPLinkProperties()
    {
        super();
        this.maxMessageSize = 0L;
        this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED_STR;
        this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND_STR;
        deliveryPolicy = MessageDeliveryPolicy.ExactlyOnce;
    }    
}
