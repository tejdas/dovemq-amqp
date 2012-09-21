package net.dovemq.transport.link;

import java.util.Collection;

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
    
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState);

    public void sessionClosed();
    
    public LinkRole getRole();
}
