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

package net.dovemq.api;

import net.dovemq.broker.endpoint.DoveMQEndpointDriver;
import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.session.CAMQPSessionFactory;
import net.dovemq.transport.session.CAMQPSessionInterface;

import org.apache.commons.lang.StringUtils;

/**
 * This class is used to initialize and shutdown DoveMQ runtime.
 * Also used as a factory to create AMQP connections and sessions.
 *
 * @author tejdas
 */
public final class ConnectionFactory {
    private volatile static String endpointId = null;

    public static String getEndpointId() {
        return endpointId;
    }

    /**
     * Initialize DoveMQ runtime with a supplied endpointId. The endpointId
     * allows the runtime to be distinguishable from other DoveMQ endpoints on
     * the same machine, and also uniquely addressable.
     *
     * @param endpointID
     */
    public static void initialize(String endpointID) {
        if (endpointId == null) {
            if (StringUtils.isEmpty(endpointID)) {
                throw new IllegalArgumentException("Null EndpointID specified");
            }
            boolean isBroker = false;
            DoveMQEndpointDriver.initialize(isBroker, endpointID);
            endpointId = CAMQPConnectionManager.getContainerId();
        }
    }

    /**
     * Shuts down DoveMQ runtime.
     */
    public static void shutdown() {
        if (endpointId == null) {
            throw new IllegalStateException("DoveMQ Runtime has not been initialized yet");
        }
        boolean isBroker = false;
        DoveMQEndpointDriver.shutdown(isBroker);
        endpointId = null;
    }

    /**
     * Creates a new Connection to the target DoveMQ broker. Internally, it creates
     * an AMQP connection.
     *
     * @param targetDoveMQBrokerAddress
     * @return Connection, that encapsulates an AMQP connection
     */
    public static Connection createConnection(String targetDoveMQBrokerAddress) {
        if (StringUtils.isEmpty(targetDoveMQBrokerAddress)) {
            throw new IllegalArgumentException("DoveMQ Broker address must be specified");
        }
        if (endpointId == null) {
            throw new IllegalStateException("DoveMQ Runtime has not been initialized yet");
        }
        String brokerContainerId = String.format("broker@%s", targetDoveMQBrokerAddress);
        CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();

        CAMQPConnectionInterface connection = CAMQPConnectionFactory.createCAMQPConnection(brokerContainerId, connectionProps);
        return new Connection(connection);
    }

    /**
     * Creates a new Session to the target DoveMQ broker. Internally, it creates
     * an AMQP connection. It then creates an AMQP session over it.
     *
     * @param targetDoveMQBrokerAddress
     * @return Session, that encapsulates an AMQP session
     */
    public static Session createSession(String targetDoveMQBrokerAddress) {
        if (StringUtils.isEmpty(targetDoveMQBrokerAddress)) {
            throw new IllegalArgumentException("DoveMQ Broker address must be specified");
        }
        if (endpointId == null) {
            throw new IllegalStateException("DoveMQ Runtime has not been initialized yet");
        }
        String brokerContainerId = String.format("broker@%s", targetDoveMQBrokerAddress);
        CAMQPSessionInterface camqpSession = CAMQPSessionFactory.createCAMQPSession(brokerContainerId);
        return new Session(endpointId, camqpSession);
    }
}
