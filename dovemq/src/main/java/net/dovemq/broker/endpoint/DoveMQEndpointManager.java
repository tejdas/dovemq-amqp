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

package net.dovemq.broker.endpoint;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

public interface DoveMQEndpointManager
{
    public void producerAttached(String queueName, CAMQPTargetInterface producer, CAMQPEndpointPolicy endpointPolicy);
    public void producerDetached(String queueName, CAMQPTargetInterface producer);
    public void consumerAttached(String queueName, CAMQPSourceInterface consumer, CAMQPEndpointPolicy endpointPolicy);
    public void consumerDetached(String queueName, CAMQPSourceInterface consumer);

    public void publisherAttached(String topicName, CAMQPTargetInterface publisher, CAMQPEndpointPolicy endpointPolicy);
    public void publisherDetached(String topicName, CAMQPTargetInterface publisher, CAMQPEndpointPolicy endpointPolicy);
    public void subscriberAttached(String topicName, CAMQPSourceInterface subscriber, CAMQPEndpointPolicy endpointPolicy);
    public void subscriberDetached(String topicName, CAMQPSourceInterface subscriber, CAMQPEndpointPolicy endpointPolicy);
}
