package net.dovemq.transport.link;

import net.dovemq.transport.endpoint.CAMQPMessageDeliveryPolicy;
import net.dovemq.transport.protocol.data.CAMQPConstants;

public class CAMQPLinkProperties
{
    private final long maxMessageSize;
    private final String senderSettleMode;
    private final String receiverSettleMode;
    private final CAMQPMessageDeliveryPolicy deliveryPolicy;
    
    public CAMQPMessageDeliveryPolicy getDeliveryPolicy()
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
            CAMQPMessageDeliveryPolicy deliveryPolicy)
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
        deliveryPolicy = CAMQPMessageDeliveryPolicy.ExactlyOnce;
    }    
}
