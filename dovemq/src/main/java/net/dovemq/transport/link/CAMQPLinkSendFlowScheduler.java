package net.dovemq.transport.link;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

/**
 * This class is responsible for triggering CAMQPLinkSender to spontaneously
 * send Link flow frame to the AMQP link receiver asking for link-credit, if
 * the link credit is 0 or less, and there are outstanding messages to be sent.
 * Subsequent flow frames are sent with an exponential back-off delay.
 *
 * For example, while the condition still persists (sender needs to send messages
 * but does not have link credit), the first flow frame is sent after 2 seconds.
 * Then, after 4 seconds, 8 seconds, 16 seconds, 32 seconds. Thereafter the flow-frames
 * are sent every 60 seconds.
 *
 * @author tejdas
 *
 */
class CAMQPLinkSendFlowScheduler implements Runnable
{
    private static final int LINK_SENDER_REQUEST_CREDIT_TIMER_INTERVAL = 2000;
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    void start()
    {
        scheduledExecutor.scheduleWithFixedDelay(this, LINK_SENDER_REQUEST_CREDIT_TIMER_INTERVAL,
                LINK_SENDER_REQUEST_CREDIT_TIMER_INTERVAL, TimeUnit.MILLISECONDS);
    }

    void stop()
    {
        scheduledExecutor.shutdown();
    }

    private final ConcurrentMap<Long, CAMQPLinkSender> linkSenders = new ConcurrentHashMap<Long, CAMQPLinkSender>();

    void registerLinkSender(long linkHandle, CAMQPLinkSender linkSender)
    {
        linkSenders.put(linkHandle, linkSender);
    }

    void unregisterLinkSender(long linkHandle)
    {
        linkSenders.remove(linkHandle);
    }

    @Override
    public void run()
    {
        long currentTime = System.currentTimeMillis();
        Set<Long> registeredLinkSenderHandles = linkSenders.keySet();
        for (long linkHandle : registeredLinkSenderHandles)
        {
            CAMQPLinkSender linkSender = linkSenders.get(linkHandle);
            if (linkSender != null)
            {
                linkSender.requestCreditIfUnderFlowControl(currentTime);
            }
        }
    }
}
