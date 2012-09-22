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

/**
 * This class implements CAMQPSourceInterface and is responsible for keeping
 * track of message dispositions at source end-point.
 *
 * @author tejdas
 */
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

    /**
     * Called just before the message is sent. Puts the message in the
     * unsettledDeliveries map if the message delivery policy is ExactlyOnce or
     * AtleastOnce. For, AtmostOnce, it just forgets about the message after
     * sending it.
     */
    @Override
    public void messageSent(long deliveryId, CAMQPMessage message)
    {
        if (endpointPolicy.getDeliveryPolicy() != CAMQPMessageDeliveryPolicy.AtmostOnce)
            unsettledDeliveries.put(deliveryId, message);
    }

    /**
     * Processes the Collection of disposed transferIds.
     */
    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
    {
        /*
         * In the case of AtmostOnce delivery policy, the messages are already
         * deemed settled by the source at the point of sending, so nothing
         * needs to be done.
         */
        if (endpointPolicy.getDeliveryPolicy() == CAMQPMessageDeliveryPolicy.AtmostOnce)
        {
            return deliveryIds;
        }

        /*
         * In the case of ExactlyOnce delivery policy, the target settles the
         * message only after the source settles it. It means that the message
         * is no longer in the unsettled map, if isMessageSettledByPeer. So,
         * nothing needs to be done here.
         */
        if (isMessageSettledByPeer && (endpointPolicy.getDeliveryPolicy() == CAMQPMessageDeliveryPolicy.ExactlyOnce))
        {
            return deliveryIds;
        }

        /*
         * Process the unsettled messages
         */
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

        /*
         * Settle the transferIds
         */
        boolean settled = true;
        for (long settledDeliveryId : settledDeliveryIds)
        {
            CAMQPLinkEndpoint linkEndpoint = (CAMQPLinkEndpoint) linkSender;
            linkEndpoint.sendDisposition(settledDeliveryId,
                    settled,
                    new CAMQPDefinitionAccepted());
        }

        if (!settledDeliveryIds.isEmpty())
        {
            deliveryIds.removeAll(settledDeliveryIds);
        }
        /*
         * At this point, the collection deliveryIds contains transferIds that
         * have not originated at this endpoint.
         */
        return deliveryIds;
    }

    @Override
    public void sendMessage(CAMQPMessagePayload message)
    {
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(deliveryTag, message);
    }
}
