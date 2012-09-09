package net.dovemq.transport.link;

import java.util.Collection;

public interface LinkCommandMBean
{  
    public void registerFactory(String factoryName);
    public void registerSource(String linkSource, String linkTarget, long initialMessageCount);
    public void registerTarget(String linkSource, String linkTarget);
    public Collection<String> getLinks();
    public void createSenderLink(String source, String target, String remoteContainerId);
    public void setLinkCreditSteadyState(String linkName, long minLinkCreditThreshold, long linkCreditBoost);
    public void issueLinkCredit(String linkName, long linkCreditBoost);
    public long getNumMessagesReceived();
    public void reset();
    
    public void attachTarget(String linkSource, String linkTarget);
}