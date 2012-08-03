package net.dovemq.transport.link;

public interface CAMQPSourceInterface
{
    public CAMQPMessage getMessage();
    public long getMessageCount();
    public void messageStateChanged(String deliveryId, int oldState, int newState);
}
