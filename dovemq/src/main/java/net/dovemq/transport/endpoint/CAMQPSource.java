package net.dovemq.transport.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkEndpoint;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.link.CAMQPMessage;

class CAMQPSource implements CAMQPSourceInterface
{
    private final CAMQPLinkSenderInterface linkSender;
    private final Map<Long, CAMQPMessage> unsettledDeliveries = new ConcurrentHashMap<Long, CAMQPMessage>();
    
    CAMQPSource(CAMQPLinkSenderInterface linkSender)
    {
        super();
        this.linkSender = linkSender;
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
        unsettledDeliveries.put(deliveryId, message);
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean settleMode, Object newState)
    {
        if (settleMode)
        {
            return deliveryIds;
        }
        List<Long> settledDeliveryIds = new ArrayList<Long>();
        for (long deliveryId : deliveryIds)
        {
            CAMQPMessage message = unsettledDeliveries.remove(deliveryId);
            if (message != null)
            {
                settledDeliveryIds.add(deliveryId);
            }
        }
        
        for (long settledDeliveryId : settledDeliveryIds)
        {
            CAMQPLinkEndpoint linkEndpoint = (CAMQPLinkEndpoint) linkSender;
            linkEndpoint.sendDisposition(settledDeliveryId, true, newState);
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
