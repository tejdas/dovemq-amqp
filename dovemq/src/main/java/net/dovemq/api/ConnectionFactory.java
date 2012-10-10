package net.dovemq.api;

import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.session.CAMQPSessionFactory;
import net.dovemq.transport.session.CAMQPSessionInterface;

public final class ConnectionFactory
{
    private volatile String endpointId = null;
    public String getEndpointId()
    {
        return endpointId;
    }

    /**
     * Initialize DoveMQ runtime with a supplied endpointId.
     * The endpointId allows the runtime to be distinguishable
     * from other DoveMQ endpoints on the same machine, and also
     * uniquely addressable.
     *
     * @param endpointID
     */
    public void initialize(String endpointID)
    {
        if (endpointId != null)
        {
            boolean isBroker = false;
            CAMQPLinkManager.initialize(isBroker, endpointID);
            endpointId = CAMQPConnectionManager.getContainerId();
        }
    }

    /**
     * Shuts down DoveMQ runtime.
     */
    public void shutdown()
    {
        if (endpointId != null)
        {
            CAMQPLinkManager.shutdown();
        }
    }

    public Session createSession(String targetDoveMQBrokerAddress)
    {
        String brokerContainerId = String.format("broker@%s", targetDoveMQBrokerAddress);
        CAMQPSessionInterface camqpSession = CAMQPSessionFactory.createCAMQPSession(brokerContainerId);
        return new Session(camqpSession);
    }
}
