package net.dovemq.transport.connection;

public class ConnectionObserver implements CAMQPConnectionObserver
{
    @Override
    public void connectionAccepted(CAMQPConnection connection)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void connectionCloseInitiatedByRemotePeer(CAMQPConnection connection)
    {
        System.out.println("connectionCloseInitiatedByRemotePeer : closing connection : " + connection.getRemoteContainerId());
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        connection.closeAsync();
    }
}
