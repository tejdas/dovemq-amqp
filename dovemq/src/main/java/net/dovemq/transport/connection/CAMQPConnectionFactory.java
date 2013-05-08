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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.dovemq.transport.utils.CAMQPThreadFactory;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * Factory for creating AMQP Connection
 * @author tejdas
 *
 */
public final class CAMQPConnectionFactory {
    private static final Logger log = Logger.getLogger(CAMQPConnectionFactory.class);

    private static final CAMQPConnectionFactory connectionFactory = new CAMQPConnectionFactory();

    private static final AtomicBoolean doShutdown = new AtomicBoolean(false);

    private final ClientBootstrap bootstrap;

    public static CAMQPConnectionInterface createCAMQPConnection(String targetHostName, CAMQPConnectionProperties connectionProps) {
        return connectionFactory.createConnection(targetHostName, connectionProps);
    }

    public static CAMQPConnectionInterface createCAMQPConnectionToEndpoint(String targetHostName, int port, CAMQPConnectionProperties connectionProps) {
        return connectionFactory.createConnection(targetHostName, port, connectionProps);
    }

    public static void shutdown() {
        if (doShutdown.compareAndSet(false, true)) {
            connectionFactory.shutdownFactory();
        }
        else {
            throw new IllegalArgumentException("CAMQPConnectionFactory already shutdown");
        }
    }

    private CAMQPConnectionFactory() {
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(new CAMQPThreadFactory(
                        "DoveMQNettyBossThread")),
                Executors.newCachedThreadPool(new CAMQPThreadFactory(
                        "DoveMQNettyWorkerThread"))));

        bootstrap.setPipelineFactory(new CAMQPConnectionPipelineFactory(true, null));
    }

    private CAMQPConnection createConnection(String targetContainerId, CAMQPConnectionProperties connectionProps) {
        return createConnection(targetContainerId, CAMQPConnectionConstants.AMQP_IANA_PORT, connectionProps);
    }

    private CAMQPConnection createConnection(String targetContainerId, int port, CAMQPConnectionProperties connectionProps) {
        if (-1 == targetContainerId.indexOf('@')) {
            String errorInfo = String.format("Malformed containerID (%s), target Host could not be determined", targetContainerId);
            log.fatal(errorInfo);
            throw new IllegalArgumentException(errorInfo);
        }
        String targetHostName = targetContainerId.split("@")[1];

        ChannelFuture connectFuture = null;
        InetSocketAddress remoteAddress = new InetSocketAddress(targetHostName, port);
        if (remoteAddress.isUnresolved()) {
            String targetHostNameUnqualified = targetHostName.split("\\.")[0];
            remoteAddress = new InetSocketAddress(targetHostNameUnqualified, port);
            if (remoteAddress.isUnresolved()) {
                String errorMessage = String.format("Could not resolve remote address to endpoint: %s", targetHostName);
                log.error(errorMessage);
                throw new CAMQPConnectionException(errorMessage);
            }
        }
        connectFuture = bootstrap.connect(remoteAddress);

        Channel channel = connectFuture.awaitUninterruptibly().getChannel();
        if ((channel == null) || (!channel.isConnected())) {
            String errorMessage = String.format("Connection could not be established to endpoint: %s", targetHostName);
            log.error(errorMessage);
            throw new CAMQPConnectionException(errorMessage, connectFuture.getCause());
        }

        /*
         * Instantiate a sister incoming handler from the pipeline to receive
         * data from AMQP peer
         */
        CAMQPConnectionHandler handler = channel.getPipeline()
                .get(CAMQPConnectionHandler.class);

        CAMQPConnection amqpConnection = new CAMQPConnection(handler.getStateActor());
        /*
         * Initiate handshake and wait for handshake complete TODO handshake
         * timeout
         */
        amqpConnection.initialize(channel, connectionProps);
        amqpConnection.waitForReady();
        return amqpConnection;
    }

    private void shutdownFactory() {
        // Shut down all thread pools to exit.
        bootstrap.releaseExternalResources();
    }
}
