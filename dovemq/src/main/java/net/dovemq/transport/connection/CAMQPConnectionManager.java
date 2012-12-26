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

package net.dovemq.transport.connection;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.session.CAMQPSessionManager;
import net.dovemq.transport.utils.CAMQPThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Manages outstanding AMQP connections
 * @author tejdas
 *
 */
public final class CAMQPConnectionManager {
    private static final Logger log = Logger.getLogger(CAMQPConnectionManager.class);

    private static final int DEFAULT_HEARTBEAT_PROCESSOR_THREAD_COUNT = 4;

    private static volatile CAMQPConnectionObserver connectionObserver = null;

    private static volatile String containerId = null;

    private static volatile boolean shutdownInProgress = false;

    private static final ConcurrentMap<CAMQPConnectionKey, CAMQPConnection> openConnections =
            new ConcurrentHashMap<CAMQPConnectionKey, CAMQPConnection>();

    private static final Object shutdownLock = new Object();

    private static final ScheduledExecutorService connectionHeartbeatScheduler =
            Executors.newScheduledThreadPool(DEFAULT_HEARTBEAT_PROCESSOR_THREAD_COUNT,
                    new CAMQPThreadFactory("DoveMQConnectionHeartbeatProcessor"));

    static ScheduledExecutorService getConnectionHeartbeatScheduler() {
        return connectionHeartbeatScheduler;
    }

    public synchronized static void initialize(String containerId) {
        if (CAMQPConnectionManager.containerId == null) {
            String hostName = "localhost";
            try {
                InetAddress localMachine = InetAddress.getLocalHost();
                hostName = localMachine.getHostAddress();
                log.debug("hostName: " + hostName);
            }
            catch (java.net.UnknownHostException uhe) {
                log.error("Caught UnknownHostException while resolving canonicalHostName: " + uhe.getMessage());
                // handle exception
            }
            CAMQPConnectionManager.containerId = String.format("%s@%s", containerId, hostName);
            log.info("Initialized DoveMQ endpoint ID: " + CAMQPConnectionManager.containerId);
            System.out.println("Initialized DoveMQ endpoint ID: " + CAMQPConnectionManager.containerId);
        }
    }

    /*
     * Used only by CAMQP functional tests
     */
    public static CAMQPConnection getCAMQPConnection(String targetContainerId) {
        Collection<CAMQPConnectionKey> keys = openConnections.keySet();
        for (CAMQPConnectionKey key : keys) {
            if (StringUtils.equalsIgnoreCase(key.getRemoteContainerId(), targetContainerId)) {
                return openConnections.get(key);
            }
        }
        return null;
    }

    public static CAMQPConnection getAnyCAMQPConnection(String targetContainerId) {
        return getCAMQPConnection(targetContainerId);
    }

    public static void registerConnectionObserver(CAMQPConnectionObserver connectionAcceptor) {
        CAMQPConnectionManager.connectionObserver = connectionAcceptor;
    }

    public static String getContainerId() {
        return containerId;
    }

    static Collection<String> listConnections() {
        Collection<String> connectionList = new ArrayList<String>();
        Set<CAMQPConnectionKey> keys = openConnections.keySet();
        for (CAMQPConnectionKey k : keys) {
            connectionList.add(k.toString());
        }
        return connectionList;
    }

    static void connectionClosed(CAMQPConnectionKey key) {
        connectionClosedInternal(key);
        CAMQPSessionManager.connectionClosed(key);
    }

    static void connectionAborted(CAMQPConnectionKey key) {
        CAMQPConnection abortedConnection = connectionClosedInternal(key);
        if (abortedConnection != null) {
            abortedConnection.aborted();
        }
        CAMQPSessionManager.connectionClosed(key);
    }

    private static CAMQPConnection connectionClosedInternal(CAMQPConnectionKey key) {
        CAMQPConnection connection = openConnections.remove(key);
        synchronized (shutdownLock) {
            if ((openConnections.size() == 0) && shutdownInProgress) {
                shutdownLock.notifyAll();
            }
        }
        return connection;
    }

    static void connectionCreated(CAMQPConnectionKey key, CAMQPConnection connection) {
        connectionCreatedInternal(key, connection);
    }

    private static void connectionCreatedInternal(CAMQPConnectionKey key, CAMQPConnection connection) {
        if (shutdownInProgress) {
            log.error("Shutdown is already in progress: cannot add CAMQPConnection to ConnectionManager's openConnectionsList");
            return; // TODO handle error
        }
        openConnections.put(key, connection);
    }

    static void connectionAccepted(CAMQPConnectionStateActor stateActor, CAMQPConnectionKey key) {
        CAMQPConnection amqpConnection = new CAMQPConnection(stateActor);
        connectionCreatedInternal(key, amqpConnection);
        if (connectionObserver != null) {
            connectionObserver.connectionAccepted(amqpConnection);
        }
    }

    public static void connectionCloseInitiatedByRemotePeer(CAMQPConnectionKey key) {
        if (connectionObserver != null) {
            CAMQPConnection connection = openConnections.get(key);
            if (connection != null) {
                connectionObserver.connectionCloseInitiatedByRemotePeer(connection);
            }
            else {
                log.warn("Connection not found in the openConnections list: " + key.getRemoteContainerId() + "  " + key.getEphemeralPort());
            }
        }
    }

    public static void shutdown() {
        if (shutdownInProgress) {
            return;
        }
        shutdownInProgress = true;

        Collection<CAMQPConnectionKey> connectionKeys = openConnections.keySet();
        for (CAMQPConnectionKey connectionKey : connectionKeys) {
            CAMQPConnection connection = openConnections.get(connectionKey);
            if (connection != null) {
                connection.closeAsync();
            }
        }
        try {
            synchronized (shutdownLock) {
                while (openConnections.size() > 0) {
                    shutdownLock.wait();
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        connectionHeartbeatScheduler.shutdown();
        try {
            connectionHeartbeatScheduler.awaitTermination(300, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Shutdown DoveMQ endpoint ID: " + containerId);
        System.out.println("Shutdown DoveMQ endpoint ID: " + containerId);
    }
}
