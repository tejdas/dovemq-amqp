package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public interface CAMQPLinkMessageHandler
{
    public void attachReceived(CAMQPControlAttach controlFrame);
    
    public void detachReceived(CAMQPControlDetach controlFrame);

    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload);
    
    public void flowReceived(CAMQPControlFlow flow);

    public void sessionClosed();
}
