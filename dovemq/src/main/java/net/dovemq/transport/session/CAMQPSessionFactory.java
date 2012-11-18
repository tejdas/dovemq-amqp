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

import java.util.List;

/**
 * Factory to create AMQP sessions
 * @author tejdas
 *
 */
public final class CAMQPSessionFactory
{
    private static final CAMQPSessionFactory sessionFactory = new CAMQPSessionFactory();

    private CAMQPSessionFactory()
    {
    }

    /**
     * If a session already exists to the AMQP target, return it. Otherwise, create
     * a new session.
     *
     * @param targetContainerId
     * @return
     */
    public static CAMQPSessionInterface getOrCreateCAMQPSession(String targetContainerId)
    {
        List<CAMQPSession> sessionList = CAMQPSessionManager.getAllSessions(targetContainerId);
        if (sessionList.isEmpty())
        {
            return sessionFactory.createSession(targetContainerId, false);
        }

        return sessionList.get(0); // TODO get the session with minimal linkReceivers attached
    }

    public static CAMQPSessionInterface createCAMQPSession(String targetContainerId)
    {
         return sessionFactory.createSession(targetContainerId, false);
    }

    public static CAMQPSessionInterface createCAMQPSession(String targetContainerId, boolean exclusiveConnection)
    {
         return sessionFactory.createSession(targetContainerId, true);
    }

    private CAMQPSession createSession(String targetContainerId, boolean exclusiveConnection)
    {
        CAMQPSession session = new CAMQPSession();
        session.open(targetContainerId, exclusiveConnection);
        return session;
    }
}
