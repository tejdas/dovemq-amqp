package net.dovemq.transport.link;

import java.util.Collection;

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.ReceiverLinkCreditPolicy;

public interface LinkCommandMBean
{
    public void registerFactory(String factoryName);
    public void registerSource(String linkSource, String linkTarget, long initialMessageCount);
    public void registerTarget(String linkSource, String linkTarget);
    public void registerDelayedTarget(String linkSource, String linkTarget, int averageMsgProcessingTime);
    public Collection<String> getLinks();
    public void createSenderLink(String source, String target, String remoteContainerId);
    public void setLinkCreditSteadyState(String linkName, long minLinkCreditThreshold, long linkCreditBoost, ReceiverLinkCreditPolicy policy);
    public void issueLinkCredit(String linkName, long linkCreditBoost);
    public long getNumMessagesReceived();
    public long getNumMessagesProcessedByDelayedEndpoint();
    public boolean processedAllMessages();
    public void reset();

    public void attachTarget(String linkSource, String linkTarget);
    public void attachSharedTarget(String linkSource, String linkTarget);
    public long getNumMessagesReceivedAtTargetReceiver();

    public void createSource(String source, String target, String remoteContainerId);
}
