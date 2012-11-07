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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;
import net.dovemq.transport.link.CAMQPLinkSenderFlowControlException;

import org.apache.log4j.Logger;

final class QueueRouter implements CAMQPMessageReceiver, CAMQPMessageDispositionObserver
{
    private static final Logger log = Logger.getLogger(QueueRouter.class);

    public QueueRouter(String queueName)
    {
        super();
        this.queueName = queueName;
    }

    private final String queueName;
    private final Queue<DoveMQMessage> messageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private final Queue<DoveMQMessage> inFlightMessageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private final Queue<CAMQPSourceInterface> consumerProxies = new LinkedList<CAMQPSourceInterface>();
    private CAMQPTargetInterface producerSink = null;
    private boolean sendInProgress = false;

    private CAMQPSourceInterface getNextDestination()
    {
        if (consumerProxies.isEmpty())
        {
            return null;
        }
        else if (consumerProxies.size() == 1)
        {
            return consumerProxies.peek();
        }
        else
        {
            CAMQPSourceInterface nextDestination = consumerProxies.poll();
            consumerProxies.add(nextDestination);
            return nextDestination;
        }
    }

    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target)
    {
        if (!messageQueue.add(message))
        {
            // TODO queue full
        }

        CAMQPSourceInterface currentDestination = null;
        synchronized (this)
        {
            if (sendInProgress || (consumerProxies.isEmpty()))
            {
                return;
            }
            sendInProgress = true;
            currentDestination = getNextDestination();
        }

        sendMessages(currentDestination);
    }

    private void sendMessages(CAMQPSourceInterface currentDestination)
    {
        while (true)
        {
            DoveMQMessage messageToSend = messageQueue.poll();
            if (messageToSend == null)
            {
                break;
            }

            inFlightMessageQueue.add(messageToSend);

            if (currentDestination == null)
            {
                synchronized (this)
                {
                    currentDestination = getNextDestination();
                    if (currentDestination == null)
                    {
                        sendInProgress = false;
                        return;
                    }
                }
            }

            try
            {
                currentDestination.sendMessage(messageToSend);
                currentDestination = null;
            }
            catch (RuntimeException ex)
            {
                // TODO
                if (ex instanceof CAMQPLinkSenderFlowControlException)
                {
                    inFlightMessageQueue.remove(messageToSend);
                    break;
                }
            }
        }

        synchronized (this)
        {
            sendInProgress = false;
        }
    }

    @Override
    public void messageAckedByConsumer(DoveMQMessage message)
    {
        if (inFlightMessageQueue.remove(message))
        {
            CAMQPTargetInterface sink = getSourceSink();
            if (sink != null)
            {
                long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
                sink.acknowledgeMessageProcessingComplete(deliveryId);
            }
        }
    }

    private synchronized CAMQPTargetInterface getSourceSink()
    {
        return producerSink;
    }

    void consumerAttached(CAMQPSourceInterface consumerProxy)
    {
        CAMQPSourceInterface currentDestination = null;
        synchronized(this)
        {
            if (consumerProxies.contains(consumerProxy))
            {
                return;
            }

            consumerProxies.add(consumerProxy);
            if (sendInProgress)
            {
                return;
            }
            sendInProgress = true;
            currentDestination = getNextDestination();
        }
        consumerProxy.registerDispositionObserver(this);
        sendMessages(currentDestination);
    }

    void consumerDetached(CAMQPSourceInterface consumerProxy)
    {
        synchronized(this)
        {
            consumerProxies.remove(consumerProxy);
        }
    }

    void producerAttached(CAMQPTargetInterface producerSink)
    {
        synchronized (this)
        {
            if (this.producerSink != null)
            {
                log.warn("Producer already attached to queue: " + queueName);
                return;
            }
            this.producerSink = producerSink;
        }
        producerSink.registerMessageReceiver(this);
    }

    void producerDetached(CAMQPTargetInterface producerSink)
    {
        synchronized (this)
        {
            if (this.producerSink != producerSink)
            {
                log.error("The Producer is not attached to queue: " + queueName);
            }
            else
            {
                this.producerSink = null;
            }
        }
    }

    synchronized boolean isCompletelyDetached()
    {
        return ((producerSink == null) && (consumerProxies.isEmpty()) && messageQueue.isEmpty());
    }
}
