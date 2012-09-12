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

class CAMQPTarget implements CAMQPTargetInterface
{

    private final Map<Long, CAMQPMessage> unsettledDeliveries = new ConcurrentHashMap<Long, CAMQPMessage>();
    private final CAMQPLinkReceiverInterface linkReceiver;
    
    CAMQPTarget(CAMQPLinkReceiverInterface linkReceiver)
    {
        super();
        this.linkReceiver = linkReceiver;
    }

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
         * TODO: Process the message here.
         */
        
        // send the disposition
        messageProcessingComplete(deliveryId, settled, new CAMQPDefinitionAccepted());
    }

    @Override
    public void messageStateChanged(String deliveryId,
            int oldState,
            int newState)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean settleMode, Object newState)
    {
        if (!settleMode)
        {
            return deliveryIds;
        }
        List<Long> settledDeliveryIds = new ArrayList<Long>();
        for (long deliveryId : deliveryIds)
        {
            System.out.println("processed and acked deliveryId: " + deliveryId);
            CAMQPMessage message = unsettledDeliveries.remove(deliveryId);
            if (message != null)
            {
                settledDeliveryIds.add(deliveryId);
            }
        }
        
        if (!settledDeliveryIds.isEmpty())
        {
            deliveryIds.removeAll(settledDeliveryIds);
        }
        return deliveryIds;
    }
    
    private void messageProcessingComplete(long deliveryId, boolean settled, Object settledState)
    {
        /*
         * Send disposition frame
         */
        CAMQPLinkEndpoint linkEndpoint = (CAMQPLinkEndpoint) linkReceiver;
        linkEndpoint.sendDisposition(deliveryId, settled, settledState);
    }
}
