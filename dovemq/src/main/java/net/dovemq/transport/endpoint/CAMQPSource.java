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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.dovemq.api.DoveMQMessage;
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
    private volatile CAMQPMessageDispositionObserver observer = null;

    @Override
    public void registerDispositionObserver(CAMQPMessageDispositionObserver observer)
    {
        this.observer = observer;
    }

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
                settledDeliveryIds.add(deliveryId);
                if (observer != null)
                {
                    observer.messageAckedByConsumer(message.getMessage());
                }
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
    public void sendMessage(DoveMQMessage message)
    {
        DoveMQMessageImpl messageImpl = (DoveMQMessageImpl) message;
        CAMQPMessagePayload encodedMessagePayload = messageImpl.marshal();
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(new CAMQPMessage(deliveryTag, encodedMessagePayload, message));
    }

    @Override
    public long getId()
    {
        return linkSender.getHandle();
    }
}
