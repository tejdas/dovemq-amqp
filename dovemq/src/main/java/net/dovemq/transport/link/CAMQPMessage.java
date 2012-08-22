package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.jcip.annotations.Immutable;

@Immutable
public class CAMQPMessage
{
    public String getDeliveryTag()
    {
        return deliveryTag;
    }
    public CAMQPMessagePayload getPayload()
    {
        return payload;
    }
    public CAMQPMessage(String deliveryTag, CAMQPMessagePayload payload)
    {
        super();
        this.deliveryTag = deliveryTag;
        this.payload = payload;
    }
    private final String deliveryTag;
    private final CAMQPMessagePayload payload;
}
