package net.dovemq.transport.link;

public interface CAMQPLinkReceiverInterface
{
    public void issueLinkCredit(long linkCreditBoost);
    public void getMessages(int messageCount);
    public void flowMessages(long minLinkCreditThreshold, long linkCreditBoost);

    public void configureSteadyStatePacedByMessageProcessing(long minLinkCreditThreshold,
            long linkCreditBoost);
    public void stop();
    public void acnowledgeMessageProcessingComplete();
}
