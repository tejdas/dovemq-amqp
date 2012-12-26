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

package net.dovemq.transport.session;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.utils.CAMQPThreadFactory;

final class CAMQPSessionSendFlowScheduler implements Runnable {
    private final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    new CAMQPThreadFactory("DoveMQSessionSendFlowScheduler"));

    void start() {
        scheduledExecutor.scheduleWithFixedDelay(
                this,
                CAMQPSessionConstants.SESSION_SENDER_REQUEST_CREDIT_TIMER_INTERVAL,
                CAMQPSessionConstants.SESSION_SENDER_REQUEST_CREDIT_TIMER_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    void stop() {
        scheduledExecutor.shutdown();
        try {
            scheduledExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final ConcurrentMap<String, CAMQPSession> sessions = new ConcurrentHashMap<String, CAMQPSession>();

    void registerSession(String sessionId, CAMQPSession session) {
        sessions.put(sessionId, session);
    }

    void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public void run() {
        Date currentTime = new Date();
        Set<String> registeredSessionIds = sessions.keySet();
        for (String sessionId : registeredSessionIds) {
            CAMQPSession session = sessions.get(sessionId);
            if (session != null) {
                session.requestOrProvideCreditIfUnderFlowControl(currentTime);
            }
        }
    }
}
