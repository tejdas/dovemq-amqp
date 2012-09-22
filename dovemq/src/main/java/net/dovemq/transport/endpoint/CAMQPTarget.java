package net.dovemq.transport.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkEndpoint;
import net.dovemq.transport.link.CAMQPLinkReceiverInterface;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;

/**
 * This class implements CAMQPTargetInterface and is responsible for keeping
 * track of message dispositions at target end-point.
 *
 * @author tejdas
 */
class CAMQPTarget implements CAMQPTargetInterface
{

    private final Map<Long, CAMQPMessage> unsettledDeliveries = new ConcurrentHashMap<Long, CAMQPMessage>();
    private final CAMQPLinkReceiverInterface linkReceiver;
    private final CAMQPEndpointPolicy endpointPolicy;
    private CAMQPTargetReceiver targetReceiver = null;

    CAMQPTarget(CAMQPLinkReceiverInterface linkReceiver,  CAMQPEndpointPolicy endpointPolicy)
    {
        super();
        this.linkReceiver = linkReceiver;
        this.endpointPolicy = endpointPolicy;
    }

    /**
     * Called by CAMQPLinkReceiver upon receipt of a new message. Marks the
     * message as settled if it has already been settled by the source, OR the
     * target's settle mode policy is RECEIVER_SETTLE_MODE_FIRST Puts the
     * unsettled messages to unsettled map.
     */
    @Override
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
    {
        boolean settled = false;
        if (receiverSettleMode == CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST)
        {
            // settle the message and send disposition with the settled state
            settled = true;
        }
        else
        {
            settled = settledBySender;
        }

        if (!settled)
        {
            unsettledDeliveries.put(deliveryId, new CAMQPMessage(deliveryTag, message));
        }

        /*
         * Dispatch the message to target receiver.
         */
        if (targetReceiver != null)
        {
            targetReceiver.messageReceived(message);
        }

        // if (endpointPolicy.getDeliveryPolicy() !=
        // CAMQPMessageDeliveryPolicy.AtmostOnce)
        /*
         * Do not send the disposition if it is already settled by the sender.
         */
        if (!settledBySender)
        {
            // send the disposition
            sendDisposition(deliveryId, settled, new CAMQPDefinitionAccepted());
        }
    }

    @Override
    public void messageStateChanged(String deliveryId,
            int oldState,
            int newState)
    {
        // TODO Auto-generated method stub

    }

    /**
     * Processes the Collection of disposed transferIds. Removes the settled
     * transferIds from unsettled map.
     */
    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
    {
        if (!isMessageSettledByPeer)
        {
            return deliveryIds;
        }
        List<Long> settledDeliveryIds = new ArrayList<Long>();
        for (long deliveryId : deliveryIds)
        {
            CAMQPMessage message = unsettledDeliveries.remove(deliveryId);
            if (message != null)
            {
                //System.out.println("TARGET processed disposition, settled deliveryId: " + deliveryId + "  current time: " + System.currentTimeMillis());
                settledDeliveryIds.add(deliveryId);
            }
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

    private void sendDisposition(long deliveryId, boolean settled, Object settledState)
    {
        /*
         * Send disposition frame
         */
        CAMQPLinkEndpoint linkEndpoint = (CAMQPLinkEndpoint) linkReceiver;
        linkEndpoint.sendDisposition(deliveryId, settled, settledState);
    }

    @Override
    public void registerTargetReceiver(CAMQPTargetReceiver targetReceiver)
    {
        this.targetReceiver = targetReceiver;
    }
}
