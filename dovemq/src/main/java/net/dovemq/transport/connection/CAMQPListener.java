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

import net.dovemq.transport.utils.CAMQPThreadFactory;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class CAMQPListener {
    private static final Logger log = Logger.getLogger(CAMQPListener.class);

    private volatile boolean hasShutdown = false;

    private final String listenAddress = "0.0.0.0";

    private final CAMQPConnectionProperties defaultConnectionProps;

    private ChannelFactory factory = null;

    private volatile Channel serverChannel = null;

    private CAMQPConnectionPipelineFactory pipelineFactory = null;

    public static CAMQPListener createCAMQPListener(CAMQPConnectionProperties defaultConnectionProps) {
        return new CAMQPListener(defaultConnectionProps);
    }

    private CAMQPListener(CAMQPConnectionProperties defaultConnectionProps) {
        this.defaultConnectionProps = defaultConnectionProps;
    }

    public void start() {
        start(CAMQPConnectionConstants.AMQP_IANA_PORT);
    }

    public void start(int listenPort) {
        factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(new CAMQPThreadFactory("DoveMQNettyBossThread")),
                        Executors.newCachedThreadPool(new CAMQPThreadFactory("DoveMQNettyWorkerThread")));

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        pipelineFactory = new CAMQPConnectionPipelineFactory(false, defaultConnectionProps);
        bootstrap.setPipelineFactory(pipelineFactory);
        try {
        serverChannel = bootstrap.bind(new InetSocketAddress(listenAddress, listenPort));
        } catch (ChannelException e) {
            throw new CAMQPConnectionException("Failed to start AMQP listener on port: " + listenPort + " error details: " + e.getMessage());
        }
        log.info("CAMQP Listener on port: " + listenPort);
        System.out.println("DoveMQ Listener on port: " + listenPort);
    }

    public void shutdown() {
        if (hasShutdown) {
            return;
        }
        hasShutdown = true;

        if (serverChannel != null) {
            ChannelFuture future = serverChannel.close();
            future.awaitUninterruptibly();
        }
        factory.releaseExternalResources();
        log.info("DoveMQ Listener shut down");
        System.out.println("DoveMQ Listener shut down");
    }
}
