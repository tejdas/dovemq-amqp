package net.dovemq.transport.link;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPConnectionObserver;

public class CAMQPConnectionReaper implements CAMQPConnectionObserver
{
    @Override
    public void connectionAccepted(CAMQPConnection connection)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void connectionCloseInitiatedByRemotePeer(CAMQPConnection connection)
    {
        connection.closeAsync();
    }
}