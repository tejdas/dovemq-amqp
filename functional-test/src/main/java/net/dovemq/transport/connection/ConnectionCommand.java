package net.dovemq.transport.connection;

import java.util.Collection;

public class ConnectionCommand implements ConnectionCommandMBean
{
    private volatile boolean shutdown = false;
    public boolean isShutdown()
    {
        return shutdown;
    }
    
    @Override
    public void help()
    {
        System.out.println("shutdown");
        System.out.println("list");
        System.out.println("create [targetContainerId]");
        System.out.println("close [targetContainerId]");
        System.out.println("closeAsync [targetContainerId]");
        System.out.println("isClosed [targetContainerId]");
    }
    
    @Override
    public void create(String targetContainerId)
    {
        CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
        CAMQPConnection connection = CAMQPConnectionFactory.createCAMQPConnection(targetContainerId, connectionProps);
        if (connection == null)
            System.out.println("AMQP connection could not be created");
    }
    
    @Override
    public void shutdown()
    {
        shutdown = true;
    }
    
    @Override
    public void list()
    {
        Collection<String> connectionList = CAMQPConnectionManager.listConnections();
        if (connectionList.size() > 0)
        {
            System.out.println("List of connections by targetContainerId");
            for (String conn : connectionList)
            {
                System.out.println(conn);
            }
        }
    }
    
    @Override
    public void close(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        conn.close();
    }
    
    @Override
    public boolean checkClosed(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        if ((conn == null) || conn.isClosed())
            return true;
        else
            return false;
    }
    
    @Override
    public void closeAsync(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        conn.closeAsync();
    }
}
