package net.dovemq.transport.connection;

import java.util.Date;

import net.jcip.annotations.GuardedBy;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;

class CMQPHeartbeatProcessor implements Runnable
{
    @GuardedBy("this")
    private Date lastReceivedHeartBeatTime = null;

    private final CAMQPConnectionStateActor stateActor;

    private final CAMQPSender sender;

    CMQPHeartbeatProcessor(CAMQPConnectionStateActor stateActor, CAMQPSender sender)
    {
        super();
        this.stateActor = stateActor;
        this.sender = sender;
    }

    synchronized void receivedHeartbeat()
    {
        lastReceivedHeartBeatTime = new Date();
    }

    @Override
    public void run()
    {
        boolean heartBeatDelayed = false;
        Date now = new Date();
        synchronized (this)
        {
            if (lastReceivedHeartBeatTime != null)
            {
                heartBeatDelayed = (now.getTime() - lastReceivedHeartBeatTime.getTime() > 2 * CAMQPConnectionConstants.HEARTBEAT_PERIOD);
            }
        }

        if (heartBeatDelayed)
        {
            stateActor.notifyHeartbeatDelay();
        }
        else
        {
            CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
            frameHeader.setChannelNumber((short) 0);
            frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE);
            ChannelBuffer header = CAMQPFrameHeaderCodec.encode(frameHeader);
            sender.sendBuffer(header, CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        }
    }
}
