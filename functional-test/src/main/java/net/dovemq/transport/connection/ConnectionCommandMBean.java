package net.dovemq.transport.connection;

public interface ConnectionCommandMBean
{
    public void help();    
    public void create(String targetContainerId);
    public void shutdown();
    public void list();
    public void close(String targetContainerId);
    public void closeAsync(String targetContainerId);
    public boolean checkClosed(String targetContainerId);
}
