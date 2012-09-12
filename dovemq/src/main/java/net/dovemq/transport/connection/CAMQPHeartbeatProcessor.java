package net.dovemq.transport.connection;

import java.util.Date;
import net.jcip.annotations.GuardedBy;
import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;

/**
 * Sends heart-beats to AMQP peer. Also detects if there are any
 * delays in incoming heart-beats.
 * 
 * @author tejdas
 *
 */
class CAMQPHeartbeatProcessor implements Runnable
{
    @GuardedBy("this")
    private Date lastReceivedHeartBeatTime = null;

    private CAMQPConnectionStateActor stateActor = null;
    private CAMQPSender sender = null;

    CAMQPHeartbeatProcessor(CAMQPConnectionStateActor stateActor, CAMQPSender sender)
    {
        super();
        this.stateActor = stateActor;
        this.sender = sender;
    }

    synchronized void receivedHeartbeat()
    {
        lastReceivedHeartBeatTime = new Date();
    }
    
    /**
     * Even after the scheduled timer encapsulating CAMQPHeartbeatProcessor is cancelled,
     * a reference to it is still held by CAMQPConnectionManager._scheduledExecutor. As
     * a result, CAMQPHeartbeatProcessor is still around even after the AMQP connection is
     * closed. The following hack is to at least ensure that the references to CAMQPConnectionStateActor
     * and CAMQPSender is released, so they are not around after the connection closure.
     */
    synchronized void done()
    {
        stateActor = null;
        sender = null;
    }

    @Override
    public void run()
    {
        boolean heartBeatDelayed = false;
        Date now = new Date();
        
        CAMQPConnectionStateActor localStateActor = null;
        CAMQPSender localSender = null;        
        synchronized (this)
        {
            if (lastReceivedHeartBeatTime != null)
            {
                heartBeatDelayed = (now.getTime() - lastReceivedHeartBeatTime.getTime() > 2 * CAMQPConnectionConstants.HEARTBEAT_PERIOD);
            }
            localStateActor = this.stateActor;
            localSender = this.sender;
        }
        
        if ((localStateActor == null) || (sender == null))
        {
            /*
             * HeartbeatProcessor already shutdown
             */
            return;
        }

        if (heartBeatDelayed)
        {
            localStateActor.notifyHeartbeatDelay();                
        }
        else
        {
            CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
            frameHeader.setChannelNumber((short) 0);
            frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE);
            ChannelBuffer header = CAMQPFrameHeaderCodec.encode(frameHeader);
            localSender.sendBuffer(header, CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        }
    }
}
