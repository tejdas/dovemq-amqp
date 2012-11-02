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

public class DoveMQEndpointManagerImpl implements DoveMQEndpointManager
{
    private final ConcurrentMap<String, PointToPointRouter> pointToPointRouters = new ConcurrentHashMap<String, PointToPointRouter>();

    @Override
    public void publisherAttached(String queueName, CAMQPTargetInterface source)
    {
        PointToPointRouter queueProcessor = pointToPointRouters.get(queueName);
        if (queueProcessor == null)
        {
            queueProcessor = new PointToPointRouter();
            queueProcessor.sourceAttached(source);
            pointToPointRouters.put(queueName,  queueProcessor);
        }
        else
        {
            queueProcessor.sourceAttached(source);
        }
    }

    @Override
    public void publisherDetached(String queueName)
    {
        PointToPointRouter queueProcessor = pointToPointRouters.get(queueName);
        if (queueProcessor != null)
        {
            queueProcessor.sourceDetached();
            if (queueProcessor.isCompletelyDetached())
            {
                pointToPointRouters.remove(queueName);
            }
        }
    }

    @Override
    public void consumerAttached(String queueName, CAMQPSourceInterface target)
    {
        PointToPointRouter queueProcessor = pointToPointRouters.get(queueName);
        if (queueProcessor == null)
        {
            queueProcessor = new PointToPointRouter();
            queueProcessor.destinationAttached(target);
            pointToPointRouters.put(queueName,  queueProcessor);
        }
        else
        {
            queueProcessor.destinationAttached(target);
        }
    }

    @Override
    public void consumerDetached(String queueName)
    {
        PointToPointRouter queueProcessor = pointToPointRouters.get(queueName);
        if (queueProcessor != null)
        {
            queueProcessor.destinationDetached();
            if (queueProcessor.isCompletelyDetached())
            {
                pointToPointRouters.remove(queueName);
            }
        }
    }
}
