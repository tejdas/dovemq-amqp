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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.link.CAMQPLinkMessageHandlerFactory;

import org.apache.log4j.Logger;

public class CAMQPSessionManager
{
    private static final Logger log = Logger.getLogger(CAMQPSessionManager.class);

    private static final CAMQPSessionManager _sessionManager = new CAMQPSessionManager();;

    private static ExecutorService executor = null;

    private static volatile long maxOutgoingWindowSize = CAMQPSessionConstants.DEFAULT_OUTGOING_WINDOW_SIZE;

    public static long getMaxOutgoingWindowSize()
    {
        return maxOutgoingWindowSize;
    }

    public static long getMaxIncomingWindowSize()
    {
        return maxIncomingWindowSize;
    }

    private static volatile long maxIncomingWindowSize = CAMQPSessionConstants.DEFAULT_INCOMING_WINDOW_SIZE;

    public static void setMaxSessionWindowSize(long maxOutgoingWindowSize,
            long maxIncomingWindowSize)
    {
        CAMQPSessionManager.maxOutgoingWindowSize = maxOutgoingWindowSize;
        CAMQPSessionManager.maxIncomingWindowSize = maxIncomingWindowSize;
    }

    public static synchronized ExecutorService getExecutor()
    {
        if (executor == null)
        {
            executor = Executors.newFixedThreadPool(32);
        }
        return executor;
    }

    public static synchronized void shutdown()
    {
        _sessionManager.closeSessions();
        if (executor != null)
        {
            executor.shutdown();
            try
            {
                executor.awaitTermination(300, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    private CAMQPLinkMessageHandlerFactory linkReceiverFactory = null;

    public static void registerLinkReceiverFactory(CAMQPLinkMessageHandlerFactory commandReceiverFactory)
    {
        _sessionManager.linkReceiverFactory = commandReceiverFactory;
    }

    protected static CAMQPLinkMessageHandlerFactory getLinkReceiverFactory()
    {
        return _sessionManager.linkReceiverFactory;
    }

    protected static CAMQPConnection getCAMQPConnection(String targetContainerId)
    {
        CAMQPConnection connection = CAMQPConnectionManager.getAnyCAMQPConnection(targetContainerId);
        if (connection == null)
        {
            CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
            connection = CAMQPConnectionFactory.createCAMQPConnection(targetContainerId, connectionProps);
        }
        return connection;
    }

    private final SortedMap<String, Map<Integer, CAMQPSession>> mappedSessions =
        new TreeMap<String, Map<Integer, CAMQPSession>>(String.CASE_INSENSITIVE_ORDER);

    protected static void sessionCreated(String amqpContainerId, int sessionChannelId, CAMQPSession session)
    {
        synchronized (_sessionManager)
        {
            Map<Integer, CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpContainerId);
            if (sessions == null)
            {
                sessions = new HashMap<Integer, CAMQPSession>();
                _sessionManager.mappedSessions.put(amqpContainerId, sessions);
            }
            sessions.put(sessionChannelId, session);
        }
    }

    protected static void sessionClosed(String amqpContainerId, int sessionChannelId)
    {
        synchronized (_sessionManager)
        {
            Map<Integer, CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpContainerId);
            if (sessions == null)
            {
                log.error("Could not find sessions for amqpContainerId: " + amqpContainerId);
                return;
            }
            if (null == sessions.remove(sessionChannelId))
            {
                log.error("Could not find session for sessionChannelId: " + sessionChannelId);
                return;
            }
        }
    }

    protected static CAMQPSession getSession(String amqpContainerId, int sessionChannelId)
    {
        synchronized (_sessionManager)
        {
            Map<Integer, CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpContainerId);
            if (sessions == null)
            {
                return null;
            }
            return sessions.get(sessionChannelId);
        }
    }

    protected static Collection<Integer> getAllAttachedChannels(String amqpContainerId)
    {
        Collection<Integer> sessionList = new ArrayList<Integer>();
        synchronized (_sessionManager)
        {
            Map<Integer, CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpContainerId);
            if (sessions != null)
            {
                Collection<Integer> sessionValues = sessions.keySet();
                sessionList.addAll(sessionValues);
            }
            return sessionList;
        }
    }

    protected static List<CAMQPSession> getAllSessions(String amqpContainerId)
    {
        List<CAMQPSession> sessionList = new ArrayList<CAMQPSession>();
        synchronized (_sessionManager)
        {
            Map<Integer, CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpContainerId);
            if (sessions != null)
            {
                Collection<CAMQPSession> sessionValues = sessions.values();
                sessionList.addAll(sessionValues);
            }
            return sessionList;
        }
    }

    private void closeSessions()
    {
        Set<String> containerIds = null;
        synchronized (_sessionManager)
        {
            containerIds = mappedSessions.keySet();
        }

        for (String containerId : containerIds)
        {
            List<CAMQPSession> sessions = getAllSessions(containerId);
            for (CAMQPSession session : sessions)
            {
                if (session != null)
                    session.close();
            }
        }
    }
}
