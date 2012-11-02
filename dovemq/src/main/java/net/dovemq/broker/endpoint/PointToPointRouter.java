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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.link.CAMQPLinkSenderFlowControlException;

class PointToPointRouter implements DoveMQMessageReceiver, CAMQPMessageDispositionObserver
{
    private final Queue<DoveMQMessage> messageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private final Queue<DoveMQMessage> inFlightMessageQueue = new ConcurrentLinkedQueue<DoveMQMessage>();
    private CAMQPSourceInterface targetProxy = null;
    private CAMQPTargetInterface sourceSink = null;
    private boolean sendInProgress = false;

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
            if (sendInProgress || (targetProxy == null))
            {
                return;
            }
            sendInProgress = true;
            currentDestination = targetProxy;
        }

        sendMessages(currentDestination);
    }

    private void sendMessages(CAMQPSourceInterface currentDestination)
    {
        try
        {
            while (true)
            {
                DoveMQMessage messageToSend = messageQueue.poll();
                if (messageToSend == null)
                {
                    break;
                }
                inFlightMessageQueue.add(messageToSend);
                try
                {
                    currentDestination.sendMessage(messageToSend);
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
        }
        finally
        {
            synchronized (this)
            {
                sendInProgress = false;
            }
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
                sink.acnowledgeMessageProcessingComplete();
            }
        }
    }

    private synchronized CAMQPTargetInterface getSourceSink()
    {
        return sourceSink;
    }

    public void destinationAttached(CAMQPSourceInterface destination)
    {
        CAMQPSourceInterface currentDestination = null;
        synchronized(this)
        {
            targetProxy = destination;
            targetProxy.registerDispositionObserver(this);
            if (sendInProgress)
            {
                return;
            }
            sendInProgress = true;
            currentDestination = destination;
        }
        sendMessages(currentDestination);
    }

    public void destinationDetached()
    {
        synchronized(this)
        {
            this.targetProxy = null;
        }
    }

    public void sourceAttached(CAMQPTargetInterface sourceSink)
    {
        synchronized (this)
        {
            this.sourceSink = sourceSink;
        }
        sourceSink.registerMessageReceiver(this);
    }

    public void sourceDetached()
    {
        synchronized (this)
        {
            sourceSink = null;
        }
    }

    public synchronized boolean isCompletelyDetached()
    {
        return ((sourceSink == null) && (targetProxy == null) && messageQueue.isEmpty());
    }
}
