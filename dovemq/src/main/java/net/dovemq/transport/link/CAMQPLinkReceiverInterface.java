package net.dovemq.transport.link;

public interface CAMQPLinkReceiverInterface
{
    public void getMessages(int messageCount);
    public void flowMessages(long minLinkCreditThreshold, long linkCreditBoost);
    public void stop();
}
