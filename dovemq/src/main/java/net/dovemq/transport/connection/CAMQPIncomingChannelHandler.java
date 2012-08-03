package net.dovemq.transport.connection;

import net.dovemq.transport.frame.CAMQPFrame;

public interface CAMQPIncomingChannelHandler
{
    public void frameReceived(CAMQPFrame frame);
    public void channelAbruptlyDetached();
}
