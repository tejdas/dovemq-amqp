package net.dovemq.transport.connection;

import java.util.Collection;

public interface ConnectionCommandMBean
{
    public void help();
    public void create(String targetContainerId);
    public void shutdown();
    public Collection<String> list();
    public void close(String targetContainerId);
    public void closeAsync(String targetContainerId);
    public boolean checkClosed(String targetContainerId);
}
