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

import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.session.CAMQPSessionInterface;

public class Session
{
    private final String brokerContainerId;
    private final CAMQPSessionInterface session;

    Session(String brokerContainerId, CAMQPSessionInterface session)
    {
        super();
        this.brokerContainerId = brokerContainerId;
        this.session = session;
    }

    public void close()
    {
        session.close();
    }

    public Producer createProducer(String queueName)
    {
        String source = "src";
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId, source, queueName, new CAMQPEndpointPolicy());
        return null;
    }

    public Consumer createConsumer(String queueName)
    {
        return null;
    }

    public Publisher createPublisher(String topicName)
    {
        return null;
    }

    public Subscriber createSubscriber(String topicName)
    {
        return null;
    }
}
