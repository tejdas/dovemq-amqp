package net.dovemq.transport.session;

import java.util.Collection;
import java.util.Random;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.link.CAMQPLinkMessageHandlerFactory;
import net.dovemq.transport.link.LinkRole;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

class MockLinkReceiverFactory implements CAMQPLinkMessageHandlerFactory
{
    @Override
    public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach)
    {
        // TODO Auto-generated method stub
        return new MockLinkReceiver(session);
    }
}

public class MockLinkReceiver implements CAMQPLinkMessageHandler, Runnable
{
    private final CAMQPSessionInterface session;
    public boolean attachReceived = false;
    public boolean detachReceived = false;
    public boolean linkFlowFrameReceived = false;
    
    public long lastTransferIdReceived = 0;
    public long lastTransferIdAcked = 0;
    public volatile boolean shutdown = false;
    
    public MockLinkReceiver(CAMQPSessionInterface session)
    {
        super();
        this.session = session;
        CAMQPSessionReceiverTest.linkReceiver = this;
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        synchronized (this)
        {
            lastTransferIdReceived = transferId;
        }
    }

    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        linkFlowFrameReceived = true;
    }

    @Override
    public void sessionClosed()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachReceived(CAMQPControlAttach controlFrame)
    {
        attachReceived = true;
    }

    @Override
    public void detachReceived(CAMQPControlDetach controlFrame)
    {
        detachReceived = true;
    }
    
    void ackTransfers(long startId, long endId)
    {
        for (long i = startId; i < endId; i++)
        {
            session.ackTransfer(i);
        }             
    }

    @Override
    public void run()
    {
        Random r = new Random();
        boolean firstTime = true;
        while (!shutdown)
        {
            try
            {
                Thread.sleep(r.nextInt(1000) + 1000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            long lastReceived;
            long ackStart;
            synchronized (this)
            {
                if (lastTransferIdAcked == lastTransferIdReceived)
                    continue;
                lastReceived = lastTransferIdReceived;
                ackStart = (firstTime)? 0 : lastTransferIdAcked+1;
            }

            if (firstTime)
                firstTime = false;
 
            for (long i = ackStart; i <= lastReceived; i++)
            {
                session.ackTransfer(i);
            }

            synchronized (this)
            {
                lastTransferIdAcked = lastReceived;
            }
        }
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

