package net.dovemq.transport.session;

import java.util.Collection;

public interface SessionCommandMBean
{  
    public void registerFactory(String factoryName);
    public void sessionCreate(String targetContainerId);
    public void sessionCreateMT(String targetContainerId, int numThreads);
    public Collection<Integer> getChannelId(String targetContainerId);
    public void sessionIO(String targetContainerId, String source, String dest);
    public void sessionClose(String targetContainerId, int channelId);
    public boolean isIODone();
}
