package net.dovemq.transport.connection;

import net.dovemq.transport.frame.CAMQPFrame;
/**
 * Session layer implements this interface to receive incoming
 * session/link frames.
 * 
 * @author tejdas
 *
 */
public interface CAMQPIncomingChannelHandler
{
    public void frameReceived(CAMQPFrame frame);
    public void channelAbruptlyDetached();
}
