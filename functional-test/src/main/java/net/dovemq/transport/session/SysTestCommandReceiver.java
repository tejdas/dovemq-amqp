package net.dovemq.transport.session;

import java.util.Collection;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.LinkRole;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public class SysTestCommandReceiver extends SysBaseLinkReceiver
{   
    public SysTestCommandReceiver(CAMQPSessionInterface session)
    {
        super(session);
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        super.transferReceived(transferId, transferFrame, payload);
        session.ackTransfer(transferId);
    }

    @Override
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds,
            boolean settleMode,
            Object newState)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public LinkRole getRole()
    {
        // TODO Auto-generated method stub
        return LinkRole.LinkReceiver;
    }    
}
