package net.dovemq.transport.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.CAMQPMessageDeliveryPolicy;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkEndpoint;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;

class CAMQPSource implements CAMQPSourceInterface
{
    private final CAMQPLinkSenderInterface linkSender;
    private final CAMQPEndpointPolicy endpointPolicy;
    private final Map<Long, CAMQPMessage> unsettledDeliveries = new ConcurrentHashMap<Long, CAMQPMessage>();
    
    CAMQPSource(CAMQPLinkSenderInterface linkSender, CAMQPEndpointPolicy endpointPolicy)
    {
        super();
        this.linkSender = linkSender;
        this.endpointPolicy = endpointPolicy;
    }

    @Override
    public CAMQPMessage getMessage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getMessageCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void messageStateChanged(String deliveryId,
            int oldState,
            int newState)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void messageSent(long deliveryId, CAMQPMessage message)
    {
        if (endpointPolicy.getDeliveryPolicy() != CAMQPMessageDeliveryPolicy.AtmostOnce)
            unsettledDeliveries.put(deliveryId, message);
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
    {
        if (endpointPolicy.getDeliveryPolicy() == CAMQPMessageDeliveryPolicy.AtmostOnce)
        {
            return deliveryIds;
        }
        
        if (isMessageSettledByPeer && (endpointPolicy.getDeliveryPolicy() == CAMQPMessageDeliveryPolicy.ExactlyOnce))
        {
            return deliveryIds;
        }        

        List<Long> settledDeliveryIds = new ArrayList<Long>();
        for (long deliveryId : deliveryIds)
        {
            CAMQPMessage message = unsettledDeliveries.remove(deliveryId);
            if (message != null)
            {
                //System.out.println("SOURCE processed disposition, settled deliveryId and acking: " + deliveryId + "  current time: " + System.currentTimeMillis());
                settledDeliveryIds.add(deliveryId);
            }
        }
        
        for (long settledDeliveryId : settledDeliveryIds)
        {
            CAMQPLinkEndpoint linkEndpoint = (CAMQPLinkEndpoint) linkSender;
            linkEndpoint.sendDisposition(settledDeliveryId, true, new CAMQPDefinitionAccepted());
        }
        
        if (!settledDeliveryIds.isEmpty())
        {
            deliveryIds.removeAll(settledDeliveryIds);
        }
        return deliveryIds;
    }

    @Override
    public void sendMessage(CAMQPMessagePayload message)
    {
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(deliveryTag, message);
    }
}
