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

package net.dovemq.transport.link;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestDelayedTarget implements CAMQPTargetInterface
{
    public LinkTestDelayedTarget(CAMQPLinkReceiverInterface linkReceiver,
            int averageMsgProcessingTime)
    {
        super();
        this.linkReceiver = linkReceiver;
        this.msgProcessingTime = averageMsgProcessingTime;
    }

    private static class MsgDetails
    {
        public MsgDetails(long receivedTime, int messageProcessingTime)
        {
            super();
            this.receivedTime = receivedTime;
            this.messageProcessingTime = messageProcessingTime;
        }

        public long receivedTime;

        public int messageProcessingTime;

        public boolean hasExpired(long newTime)
        {
            return (newTime - receivedTime > messageProcessingTime);
        }
    }

    private final AtomicLong messageCount = new AtomicLong(0);
    private final ConcurrentMap<Long, MsgDetails> messagesBeingProcessed = new ConcurrentHashMap<Long, MsgDetails>();

    private final int msgProcessingTime;
    private final CAMQPLinkReceiverInterface linkReceiver;
    private final ScheduledExecutorService _scheduledExecutor = Executors.newScheduledThreadPool(1);

    private static final Random r = new Random();

    private class MsgProcessor implements Runnable
    {
        @Override
        public void run()
        {
            Set<Long> msgs = messagesBeingProcessed.keySet();
            long currentTime = System.currentTimeMillis();
            for (Long msg : msgs)
            {
                MsgDetails msgDetail = messagesBeingProcessed.get(msg);
                if ((msgDetail != null) && msgDetail.hasExpired(currentTime))
                {
                    messagesBeingProcessed.remove(msg);
                    linkReceiver.acknowledgeMessageProcessingComplete();
                }
            }
        }
    }

    @Override
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
    {
        messageCount.incrementAndGet();
        int msgProcessingTimeDelay = r.nextInt(msgProcessingTime) + 10;
        messagesBeingProcessed.put(deliveryId, new MsgDetails(System.currentTimeMillis(), msgProcessingTimeDelay));
    }

    public long getNumberOfMessagesReceived()
    {
        return messageCount.longValue();
    }

    public void resetNumberOfMessagesReceived()
    {
        messageCount.set(0);
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean settleMode, Object newState)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerMessageReceiver(DoveMQMessageReceiver targetReceiver)
    {
        // TODO Auto-generated method stub

    }

    public void startProcessing()
    {
        _scheduledExecutor.scheduleWithFixedDelay(new MsgProcessor(), 300, 300, TimeUnit.MILLISECONDS);
    }

    public void stopProcessing()
    {
        _scheduledExecutor.shutdown();
    }

    public boolean isDone()
    {
        return messagesBeingProcessed.isEmpty();
    }

    @Override
    public void acknowledgeMessageProcessingComplete(long deliveryId)
    {
        // TODO Auto-generated method stub
    }
}
