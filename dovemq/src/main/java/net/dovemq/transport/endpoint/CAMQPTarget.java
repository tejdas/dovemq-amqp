/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
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

    private final Map<Long, Boolean> deliveriesWaitingExplicitAck = new ConcurrentHashMap<Long, Boolean>();
    private final CAMQPLinkReceiverInterface linkReceiver;
    private final CAMQPEndpointPolicy endpointPolicy;
    private volatile CAMQPMessageReceiver messageReceiver = null;

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

        if (!settledBySender)
        {
            if (expectAck())
            {
                deliveriesWaitingExplicitAck.put(deliveryId, settled);
            }
        }

        /*
         * Dispatch the message to target receiver.
         */
        if (messageReceiver != null)
        {
            DoveMQMessageImpl decodedMessage = DoveMQMessageImpl.unmarshal(message);
            decodedMessage.setDeliveryId(deliveryId);
            messageReceiver.messageReceived(decodedMessage, this);
        }

        /*
         * Do not send the disposition if it is already settled by the sender.
         */
        if (!settledBySender)
        {
            if (!expectAck())
            {
                // send the disposition
                sendDisposition(deliveryId, settled, new CAMQPDefinitionAccepted());
            }
        }
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
    public void registerMessageReceiver(CAMQPMessageReceiver targetReceiver)
    {
        this.messageReceiver = targetReceiver;
    }

    @Override
    public void acknowledgeMessageProcessingComplete(long deliveryId)
    {
        linkReceiver.acknowledgeMessageProcessingComplete();
        if (expectAck())
        {
            Boolean settled = deliveriesWaitingExplicitAck.remove(deliveryId);
            if (settled != null)
            {
                // send the disposition
                sendDisposition(deliveryId, settled, new CAMQPDefinitionAccepted());
            }
        }
    }

    private boolean expectAck()
    {
        return ((messageReceiver != null) && (endpointPolicy.getDoveMQEndpointPolicy().getAckPolicy() == MessageAcknowledgementPolicy.CONSUMER_ACKS));
    }
}
