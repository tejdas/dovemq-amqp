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
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;
import net.dovemq.transport.link.CAMQPLinkSenderFlowControlException;

class QueueRouter implements DoveMQMessageReceiver, CAMQPMessageDispositionObserver
{
    private final Queue<DoveMQMessage> messageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private final Queue<DoveMQMessage> inFlightMessageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private final Queue<CAMQPSourceInterface> targetProxies = new LinkedList<CAMQPSourceInterface>();
    private CAMQPTargetInterface sourceSink = null;
    private boolean sendInProgress = false;

    private CAMQPSourceInterface getNextDestination()
    {
        if (targetProxies.isEmpty())
        {
            return null;
        }
        else if (targetProxies.size() == 1)
        {
            return targetProxies.peek();
        }
        else
        {
            CAMQPSourceInterface nextDestination = targetProxies.poll();
            targetProxies.add(nextDestination);
            return nextDestination;
        }
    }

    @Override
    public void messageReceived(DoveMQMessage message)
    {
        if (!messageQueue.add(message))
        {
            // TODO queue full
        }

        CAMQPSourceInterface currentDestination = null;
        synchronized (this)
        {
            if (sendInProgress || (targetProxies.isEmpty()))
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
        return sourceSink;
    }

    void destinationAttached(CAMQPSourceInterface destination)
    {
        destination.registerDispositionObserver(this);

        CAMQPSourceInterface currentDestination = null;
        synchronized(this)
        {
            targetProxies.add(destination);
            if (sendInProgress)
            {
                return;
            }
            sendInProgress = true;
            currentDestination = getNextDestination();
        }
        sendMessages(currentDestination);
    }

    void destinationDetached(CAMQPSourceInterface targetProxy)
    {
        synchronized(this)
        {
            targetProxies.remove(targetProxy);
        }
    }

    void sourceAttached(CAMQPTargetInterface sourceSink)
    {
        synchronized (this)
        {
            this.sourceSink = sourceSink;
        }
        sourceSink.registerMessageReceiver(this);
    }

    void sourceDetached(CAMQPTargetInterface source)
    {
        synchronized (this)
        {
            sourceSink = null;
        }
    }

    synchronized boolean isCompletelyDetached()
    {
        return ((sourceSink == null) && (targetProxies.isEmpty()) && messageQueue.isEmpty());
    }
}
