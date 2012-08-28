package net.dovemq.transport.session;

import net.dovemq.transport.frame.CAMQPMessagePayload;
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
}
