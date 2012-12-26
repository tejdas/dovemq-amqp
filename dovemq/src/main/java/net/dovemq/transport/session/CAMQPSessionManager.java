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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.connection.CAMQPConnectionKey;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.link.CAMQPLinkMessageHandlerFactory;
import net.dovemq.transport.utils.CAMQPThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class CAMQPSessionManager
{
    private static final Logger log = Logger.getLogger(CAMQPSessionManager.class);
    private static final int DEFAULT_SESSION_DISPOSITION_SENDER_THREAD_COUNT = 8;

    private static volatile CAMQPSessionManager _sessionManager;
    private static volatile CAMQPSessionSendFlowScheduler sessionSendFlowScheduler;

    public static CAMQPSessionSendFlowScheduler getSessionSendFlowScheduler()
    {
        return sessionSendFlowScheduler;
    }

    private final ExecutorService executor = Executors.newCachedThreadPool(new CAMQPThreadFactory("DoveMQTransferFrameSenderThread"));

    private static final ScheduledExecutorService sessionSendDispositionScheduler =
            Executors.newScheduledThreadPool(DEFAULT_SESSION_DISPOSITION_SENDER_THREAD_COUNT,
                    new CAMQPThreadFactory("DoveMQSessionCumulativeDispositionSender"));


    static ScheduledExecutorService getSessionSendDispositionScheduler()
    {
        return sessionSendDispositionScheduler;
    }

    private static volatile long maxOutgoingWindowSize = CAMQPSessionConstants.DEFAULT_OUTGOING_WINDOW_SIZE;
    private static volatile long maxIncomingWindowSize = CAMQPSessionConstants.DEFAULT_INCOMING_WINDOW_SIZE;

    public static long getMaxOutgoingWindowSize()
    {
        return maxOutgoingWindowSize;
    }

    public static long getMaxIncomingWindowSize()
    {
        return maxIncomingWindowSize;
    }

    public static void setMaxSessionWindowSize(long maxOutgoingWindowSize,
            long maxIncomingWindowSize)
    {
        CAMQPSessionManager.maxOutgoingWindowSize = maxOutgoingWindowSize;
        CAMQPSessionManager.maxIncomingWindowSize = maxIncomingWindowSize;
    }

    public static ExecutorService getExecutor()
    {
        return _sessionManager.executor;
    }

    public static void initialize()
    {
        _sessionManager = new CAMQPSessionManager();
        sessionSendFlowScheduler = new CAMQPSessionSendFlowScheduler();
        sessionSendFlowScheduler.start();
    }

    public static void shutdown()
    {
        sessionSendFlowScheduler.stop();
        _sessionManager.shutdownManager();
        sessionSendFlowScheduler = null;
    }

    private void shutdownManager()
    {
        closeSessions();

        shutdownThreadPool(executor);
        shutdownThreadPool(sessionSendDispositionScheduler);
    }

    private void shutdownThreadPool(ExecutorService threadPool)
    {
        threadPool.shutdown();
        try
        {
            threadPool.awaitTermination(300, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private volatile CAMQPLinkMessageHandlerFactory linkReceiverFactory = null;

    public static void registerLinkReceiverFactory(CAMQPLinkMessageHandlerFactory commandReceiverFactory)
    {
        _sessionManager.linkReceiverFactory = commandReceiverFactory;
    }

    protected static CAMQPLinkMessageHandlerFactory getLinkReceiverFactory()
    {
        return _sessionManager.linkReceiverFactory;
    }

    protected static CAMQPConnectionInterface getCAMQPConnection(String targetContainerId)
    {
        CAMQPConnectionInterface connection = CAMQPConnectionManager.getAnyCAMQPConnection(targetContainerId);
        if (connection == null)
        {
            CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
            connection = CAMQPConnectionFactory.createCAMQPConnection(targetContainerId, connectionProps);
        }
        return connection;
    }

    protected static CAMQPConnectionInterface createCAMQPConnection(String targetContainerId)
    {
        CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
        return CAMQPConnectionFactory.createCAMQPConnection(targetContainerId, connectionProps);
    }

    private final ConcurrentMap<CAMQPConnectionKey, List<CAMQPSession>> mappedSessions =
            new ConcurrentHashMap<CAMQPConnectionKey, List<CAMQPSession>>();

    public static void connectionClosed(CAMQPConnectionKey remoteContainerId)
    {
        CAMQPSessionManager sessionManager = _sessionManager;
        if (sessionManager != null)
        {
            sessionManager.mappedSessions.remove(remoteContainerId);
        }
    }

    protected static void sessionCreated(CAMQPConnectionKey amqpRemoteConnectionKey, int sessionChannelId, CAMQPSession session)
    {
        List<CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpRemoteConnectionKey);
        List<CAMQPSession> sessionsPrevValue = null;
        if (sessions == null)
        {
            sessions = Collections.synchronizedList(new ArrayList<CAMQPSession>());
            sessionsPrevValue = _sessionManager.mappedSessions.putIfAbsent(amqpRemoteConnectionKey, sessions);
        }
        if (sessionsPrevValue == null)
        {
            sessions.add(session);
        }
        else
        {
            sessionsPrevValue.add(session);
        }
    }

    protected static void sessionClosed(CAMQPConnectionKey amqpRemoteConnectionKey, CAMQPSession session, int sessionChannelId)
    {
        List<CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpRemoteConnectionKey);
        if (sessions == null)
        {
            log.error("Could not find sessions for amqpContainerId: " + amqpRemoteConnectionKey);
            return;
        }

        if (!sessions.remove(session))
        {
            log.error("Could not find session for sessionChannelId: " + sessionChannelId);
            return;
        }
    }

    /*
     * Used only by CAMQP functional tests
     */
    protected static CAMQPSession getSession(String amqpContainerId, int sessionChannelId)
    {
        List<CAMQPSession> sessions = getAllSessions(amqpContainerId);
        if (sessions != null)
        {
            synchronized (sessions)
            {
                for (CAMQPSession session : sessions)
                {
                    if (session.getOutgoingChannelNumber() == sessionChannelId)
                    {
                        return session;
                    }
                }
            }
        }
        return null;
    }

    /*
     * Used only by CAMQP functional tests
     */
    protected static Collection<Integer> getAllAttachedChannels(String amqpContainerId)
    {
        Collection<Integer> sessionList = new ArrayList<Integer>();
        Set<CAMQPConnectionKey> amqpRemoteConnectionKeys = _sessionManager.mappedSessions.keySet();
        for (CAMQPConnectionKey key : amqpRemoteConnectionKeys)
        {
            if (StringUtils.equalsIgnoreCase(key.getRemoteContainerId(), amqpContainerId))
            {
                List<CAMQPSession> sessions = _sessionManager.mappedSessions.get(key);
                if (sessions != null)
                {
                    synchronized (sessions)
                    {
                        for (CAMQPSession session : sessions)
                        {
                            sessionList.add(session.getOutgoingChannelNumber());
                        }
                    }
                }
            }
        }
        return sessionList;
    }

    private static List<CAMQPSession> getAllSessions(CAMQPConnectionKey amqpRemoteConnectionKey)
    {
        List<CAMQPSession> sessionList = new ArrayList<CAMQPSession>();

        List<CAMQPSession> sessions = _sessionManager.mappedSessions.get(amqpRemoteConnectionKey);
        if (sessions != null)
        {
            synchronized (sessions)
            {
                sessionList.addAll(sessions);
            }
        }
        return sessionList;
    }

    protected static List<CAMQPSession> getAllSessions(String amqpContainerId)
    {
        List<CAMQPSession> sessionList = new ArrayList<CAMQPSession>();
        Set<CAMQPConnectionKey> amqpRemoteConnectionKeys = _sessionManager.mappedSessions.keySet();
        for (CAMQPConnectionKey key : amqpRemoteConnectionKeys)
        {
            if (StringUtils.equalsIgnoreCase(key.getRemoteContainerId(), amqpContainerId))
            {
                List<CAMQPSession> sessions = _sessionManager.mappedSessions.get(key);
                if (sessions != null)
                {
                    synchronized (sessions)
                    {
                        sessionList.addAll(sessions);
                    }
                }
            }
        }
        return sessionList;
    }

    private void closeSessions()
    {
        Set<CAMQPConnectionKey> amqpRemoteConnectionKeys = mappedSessions.keySet();

        for (CAMQPConnectionKey key : amqpRemoteConnectionKeys)
        {
            List<CAMQPSession> sessions = getAllSessions(key);
            for (CAMQPSession session : sessions)
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
    }
}
