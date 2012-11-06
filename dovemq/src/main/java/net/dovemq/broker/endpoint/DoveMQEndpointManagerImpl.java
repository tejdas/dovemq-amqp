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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

import org.apache.log4j.Logger;

public class DoveMQEndpointManagerImpl implements DoveMQEndpointManager
{
    private static final Logger log = Logger.getLogger(DoveMQEndpointManagerImpl.class);
    private final ConcurrentMap<String, QueueRouter> queueRouters = new ConcurrentHashMap<String, QueueRouter>();

    @Override
    public void producerAttached(String queueName, CAMQPTargetInterface publisher)
    {
        QueueRouter queueProcessor = queueRouters.get(queueName);
        if (queueProcessor == null)
        {
            queueProcessor = new QueueRouter();
            queueProcessor.sourceAttached(publisher);
            queueRouters.put(queueName,  queueProcessor);
        }
        else
        {
            queueProcessor.sourceAttached(publisher);
        }
    }

    @Override
    public void producerDetached(String queueName, CAMQPTargetInterface publisher)
    {
        QueueRouter queueProcessor = queueRouters.get(queueName);
        if (queueProcessor != null)
        {
            queueProcessor.sourceDetached(publisher);
            if (queueProcessor.isCompletelyDetached())
            {
                log.debug("Removing queue: " + queueName);
                queueRouters.remove(queueName);
            }
        }
        log.debug("Publisher detached from queue: " + queueName);
    }

    @Override
    public void consumerAttached(String queueName, CAMQPSourceInterface consumer)
    {
        QueueRouter queueProcessor = queueRouters.get(queueName);
        if (queueProcessor == null)
        {
            queueProcessor = new QueueRouter();
            queueProcessor.destinationAttached(consumer);
            queueRouters.put(queueName,  queueProcessor);
        }
        else
        {
            queueProcessor.destinationAttached(consumer);
        }
    }

    @Override
    public void consumerDetached(String queueName, CAMQPSourceInterface consumer)
    {
        QueueRouter queueProcessor = queueRouters.get(queueName);
        if (queueProcessor != null)
        {
            queueProcessor.destinationDetached(consumer);
            if (queueProcessor.isCompletelyDetached())
            {
                log.debug("Removing queue: " + queueName);
                queueRouters.remove(queueName);
            }
        }
        log.debug("Publisher detached from queue: " + queueName);
    }

    @Override
    public void publisherAttached(String topicName, CAMQPTargetInterface producer)
    {
        System.out.println("publisher attached to topic: " + topicName);
    }

    @Override
    public void publisherDetached(String topicName, CAMQPTargetInterface producer)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void subscriberAttached(String topicName, CAMQPSourceInterface consumer)
    {
        System.out.println("subscriber attached to topic: " + topicName);
    }

    @Override
    public void subscriberDetached(String topicName, CAMQPSourceInterface consumer)
    {
        // TODO Auto-generated method stub

    }
}
