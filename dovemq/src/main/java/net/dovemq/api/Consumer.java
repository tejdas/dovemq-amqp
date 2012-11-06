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

package net.dovemq.api;

import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;

public class Consumer implements CAMQPMessageReceiver
{
    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target)
    {
        doveMQMessageReceiver.messageReceived(message);
    }

    public void registerMessageReceiver(DoveMQMessageReceiver messageReceiver)
    {
        this.doveMQMessageReceiver = messageReceiver;
        targetEndpoint.registerMessageReceiver(this);
    }

    public void stop()
    {
    }

    public void acknowledge(DoveMQMessage message)
    {
        if (endpointPolicy.getAckPolicy() == MessageAcknowledgementPolicy.CONSUMER_ACKS)
        {
            long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
            targetEndpoint.acknowledgeMessageProcessingComplete(deliveryId);
        }
    }

    Consumer(String targetName, CAMQPTargetInterface targetEndpoint)
    {
        super();
        this.targetName = targetName;
        this.endpointPolicy = new DoveMQEndpointPolicy();
        this.targetEndpoint = targetEndpoint;
    }

    Consumer(String targetName, CAMQPTargetInterface targetEndpoint, DoveMQEndpointPolicy endpointPolicy)
    {
        super();
        this.targetName = targetName;
        this.endpointPolicy = endpointPolicy;
        this.targetEndpoint = targetEndpoint;
    }

    private final String targetName;
    private final DoveMQEndpointPolicy endpointPolicy;
    private final CAMQPTargetInterface targetEndpoint;
    private volatile DoveMQMessageReceiver doveMQMessageReceiver = null;
}
