package net.dovemq.transport.connection;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

public class CAMQPConnectionFactory
{
    private static final Logger log = Logger.getLogger(CAMQPConnectionFactory.class);

    private static final CAMQPConnectionFactory connectionFactory = new CAMQPConnectionFactory();

    private final ClientBootstrap bootstrap;

    public static CAMQPConnection createCAMQPConnection(String targetHostName, CAMQPConnectionProperties connectionProps)
    {
        return connectionFactory.createConnection(targetHostName, connectionProps);
    }

    public static void shutdown()
    {
        connectionFactory.shutdownFactory();
    }

    private CAMQPConnectionFactory()
    {
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new CAMQPConnectionPipelineFactory(true, null));
    }

    private CAMQPConnection createConnection(String targetContainerId, CAMQPConnectionProperties connectionProps)
    {
        if (-1 == targetContainerId.indexOf("@"))
        {
            String errorInfo = String.format("Malformed containerID (%s), target Host could not be determined", targetContainerId);
            log.fatal(errorInfo);
            throw new IllegalArgumentException(errorInfo);
        }
        String targetHostName = targetContainerId.split("@")[1];
        // Make a new connection.
        ChannelFuture connectFuture = null;
        InetSocketAddress remoteAddress = new InetSocketAddress(targetHostName, CAMQPConnectionConstants.AMQP_IANA_PORT);
        if (remoteAddress.isUnresolved())
        {
            String targetHostNameUnqualified = targetHostName.split("\\.")[0];
            remoteAddress = new InetSocketAddress(targetHostNameUnqualified, CAMQPConnectionConstants.AMQP_IANA_PORT);
            if (remoteAddress.isUnresolved())
            {
                log.error("Could not resolve remote address");
                return null;
            }
        }
        connectFuture = bootstrap.connect(remoteAddress);

        Channel channel = connectFuture.awaitUninterruptibly().getChannel();
        if ((channel == null) || (!channel.isConnected()))
        {
            log.error("Connection could not be established");
            return null;
        }

        CAMQPConnectionHandler handler = channel.getPipeline().get(CAMQPConnectionHandler.class);

        CAMQPConnection amqpConnection = new CAMQPConnection(handler.getStateActor());
        amqpConnection.initialize(channel, connectionProps);
        amqpConnection.waitForReady();
        return amqpConnection;
    }

    private void shutdownFactory()
    {
        // Shut down all thread pools to exit.
        bootstrap.releaseExternalResources();
    }
}
