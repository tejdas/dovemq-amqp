package net.dovemq.transport.connection;

public interface CAMQPConnectionObserver
{
    public void connectionAccepted(CAMQPConnection connection);
    public void connectionCloseInitiatedByRemotePeer(CAMQPConnection connection);
}
