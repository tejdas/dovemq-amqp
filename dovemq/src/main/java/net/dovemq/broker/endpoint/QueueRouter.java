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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.jcip.annotations.GuardedBy;

import org.apache.log4j.Logger;

/**
 *
 * This class is responsible for routing the messages on a queue.
 * It is attached to one instance of CAMQPTargetInterface acting as a message producer sinks
 * and one or more instances of CAMQPSourceInterface acting as message consumer proxies.
 *
 * @author tejdas
 *
 */
final class QueueRouter implements
        CAMQPMessageReceiver,
        CAMQPMessageDispositionObserver {
    private static final Logger log = Logger.getLogger(QueueRouter.class);

    private final Queue<DoveMQMessage> messageQueue = new ConcurrentLinkedQueue<>();

    private final Queue<CAMQPSourceInterface> consumerProxies = new LinkedList<>();

    private final ConcurrentMap<Long, ConcurrentMap<Long, DoveMQMessage>> inFlightMessagesByConsumerId = new ConcurrentHashMap<>();

    private CAMQPTargetInterface producerSink = null;

    private boolean sendInProgress = false;

    private final String queueName;

    public QueueRouter(String queueName) {
        super();
        this.queueName = queueName;
    }

    /**
     * Returns the next consumer proxy to send the message to. It picks up the
     * consumers in a round-robin manner.
     *
     * @return
     */
    @GuardedBy("this")
    private CAMQPSourceInterface getNextConsumerProxy() {
        if (consumerProxies.isEmpty()) {
            return null;
        }
        else if (consumerProxies.size() == 1) {
            return consumerProxies.peek();
        }
        else {
            /*
             * To achieve round-robin behavior, remove the consumer proxy from
             * the head of the queue, and add it to the tail of the queue.
             */
            CAMQPSourceInterface nextDestination = consumerProxies.poll();
            consumerProxies.add(nextDestination);
            return nextDestination;
        }
    }

    /**
     * Called by CAMQPTarget upon receipt of an incoming message.
     */
    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target) {
        if (!messageQueue.add(message)) {
            // TODO queue full
        }

        long producerId = target.getId();
        ((DoveMQMessageImpl) message).setSourceId(producerId);

        CAMQPSourceInterface currentConsumerProxy = null;
        synchronized (this) {
            if (sendInProgress || (consumerProxies.isEmpty())) {
                return;
            }
            sendInProgress = true;
            currentConsumerProxy = getNextConsumerProxy();
        }

        sendMessages(currentConsumerProxy);
    }

    /**
     * Route messages (off messageQueue) to consumer proxies.
     *
     * @param consumerProxy
     */
    private void sendMessages(CAMQPSourceInterface consumerProxy) {
        while (true) {
            DoveMQMessage messageToSend = messageQueue.peek();
            if (messageToSend == null) {
                synchronized (this) {
                    sendInProgress = false;
                    return;
                }
            }

            if (consumerProxy == null) {
                synchronized (this) {
                    consumerProxy = getNextConsumerProxy();
                    if (consumerProxy == null) {
                        sendInProgress = false;
                        return;
                    }
                }
            }

            try {
                long deliveryId = ((DoveMQMessageImpl) messageToSend).getDeliveryId();
                ConcurrentMap<Long, DoveMQMessage> messageMap = inFlightMessagesByConsumerId.get(consumerProxy.getId());
                if (messageMap != null) {
                    messageMap.put(deliveryId, messageToSend);
                }

                consumerProxy.sendMessage(messageToSend);
                consumerProxy = null;
                messageQueue.remove();
            }
            catch (RuntimeException ex) {
                log.fatal("Caught RuntimeException while routing messages off " + queueName + " to consumer proxies: " + ex.getMessage());
                synchronized (this) {
                    sendInProgress = false;
                    return;
                }
            }
        }
    }

    @Override
    public void messageAckedByConsumer(DoveMQMessage message, CAMQPSourceInterface consumer) {
        long deliveryId = ((DoveMQMessageImpl) message).getDeliveryId();
        ConcurrentMap<Long, DoveMQMessage> messageMap = inFlightMessagesByConsumerId.get(consumer.getId());
        if (messageMap != null) {
            if (messageMap.remove(deliveryId) != null) {
                acknowledgeMessageDelivered(message, deliveryId);
            }
        }
    }

    private synchronized CAMQPTargetInterface getSourceSink() {
        return producerSink;
    }

    void consumerAttached(CAMQPSourceInterface consumerProxy) {
        CAMQPSourceInterface currentDestination = null;
        synchronized (this) {
            if (consumerProxies.contains(consumerProxy)) {
                return;
            }

            consumerProxies.add(consumerProxy);
            if (!sendInProgress) {
                sendInProgress = true;
                currentDestination = getNextConsumerProxy();
            }
        }
        consumerProxy.registerDispositionObserver(this);

        ConcurrentMap<Long, DoveMQMessage> messageMap = new ConcurrentHashMap<>();
        inFlightMessagesByConsumerId.put(consumerProxy.getId(), messageMap);

        if (currentDestination != null) {
            final CAMQPSourceInterface destinationToSendMessages = currentDestination;
            DoveMQEndpointDriver.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    sendMessages(destinationToSendMessages);
                }
            });
        }
    }

    void consumerDetached(CAMQPSourceInterface consumerProxy) {
        synchronized (this) {
            consumerProxies.remove(consumerProxy);
        }
        final ConcurrentMap<Long, DoveMQMessage> messageMap = inFlightMessagesByConsumerId.remove(consumerProxy.getId());
        /*
         * Since the consumer has detached, treat the following messages as
         * having been acked and notify the producer, so its link-credit window
         * opens up. REVISIT TODO
         */
        if ((messageMap != null) && !messageMap.isEmpty()) {
            DoveMQEndpointDriver.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Set<Long> deliveryIds = messageMap.keySet();
                    for (long deliveryId : deliveryIds) {
                        DoveMQMessage message = messageMap.get(deliveryId);
                        acknowledgeMessageDelivered(message, deliveryId);
                    }
                }
            });
        }
    }

    private void acknowledgeMessageDelivered(DoveMQMessage message, long deliveryId) {
        long producerId = ((DoveMQMessageImpl) message).getSourceId();
        CAMQPTargetInterface sink = getSourceSink();
        if ((sink != null) && (sink.getId() == producerId)) {
            sink.acknowledgeMessageProcessingComplete(deliveryId);
        }
    }

    void producerAttached(CAMQPTargetInterface producerSink) {
        boolean alreadyAttached = false;
        synchronized (this) {
            if (this.producerSink != null) {
                if (this.producerSink == producerSink) {
                    log.warn("This Producer is already attached to queue: " + queueName);
                    return;
                } else {
                    log.error("Another Producer is already attached to queue: " + queueName);
                    alreadyAttached = true;
                }
            } else {
                this.producerSink = producerSink;
            }
        }

        if (alreadyAttached) {
            CAMQPDefinitionError error = new CAMQPDefinitionError();
            error.setCondition(CAMQPConstants.AMQP_ERROR_RESOURCE_LOCKED);
            error.setDescription("Another Producer is already attached to queue: " + queueName);
            producerSink.closeUnderlyingLink(error);
        } else {
            producerSink.registerMessageReceiver(this);
        }
    }

    synchronized void producerDetached(CAMQPTargetInterface producerSink) {
        if (this.producerSink == null) {
            log.error("The QueueRouter: " + queueName
                    + " is not attached to any producer");
        }
        else if (this.producerSink != producerSink) {
            log.error("The Producer: " + producerSink
                    + " is not attached to queue: "
                    + queueName
                    + " . The queue is attached by: "
                    + this.producerSink);
        }
        else {
            this.producerSink = null;
        }
    }

    synchronized boolean isCompletelyDetached() {
        return ((producerSink == null) && (consumerProxies.isEmpty()) && messageQueue.isEmpty());
    }
}
