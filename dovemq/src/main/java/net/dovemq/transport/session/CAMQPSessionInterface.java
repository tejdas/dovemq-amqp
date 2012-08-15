package net.dovemq.transport.session;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

/**
 * Interface used by the link layer to send link control frames,
 * transfer and flow frames, and also to send acknowledge frame.
 * Implemented by CAMQPSession
 * 
 * @author tejdas
 *
 */
public interface CAMQPSessionInterface
{
    public void sendLinkControlFrame(ChannelBuffer encodedLinkControlFrame);
    public void registerLinkReceiver(Long linkHandle, CAMQPLinkMessageHandler linkReceiver);
    public long getNextDeliveryId();
    public void sendTransfer(CAMQPControlTransfer transfer, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender);
    public void sendFlow(CAMQPControlFlow flow);
    
    public void ackTransfer(long transferId);
}
