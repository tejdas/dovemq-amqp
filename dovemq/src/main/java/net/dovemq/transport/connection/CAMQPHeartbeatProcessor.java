/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.connection;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;
import net.jcip.annotations.GuardedBy;

import org.jboss.netty.buffer.ChannelBuffer;

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
    private ScheduledFuture<?> scheduledFuture = null;

    void scheduleNextHeartbeat()
    {
        scheduledFuture = CAMQPConnectionManager.getConnectionHeartbeatScheduler().scheduleWithFixedDelay(this,
                CAMQPConnectionConstants.HEARTBEAT_PERIOD,
                CAMQPConnectionConstants.HEARTBEAT_PERIOD,
                TimeUnit.MILLISECONDS);
    }

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
    void stop()
    {
        if ((scheduledFuture != null) && !scheduledFuture.isCancelled())
        {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = null;

        synchronized (this)
        {
            stateActor = null;
            sender = null;
        }
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
            if ((stateActor == null) || (sender == null))
            {
                /*
                 * HeartbeatProcessor already shutdown
                 */
                return;
            }

            if (lastReceivedHeartBeatTime != null)
            {
                heartBeatDelayed = (now.getTime() - lastReceivedHeartBeatTime.getTime() > 2 * CAMQPConnectionConstants.HEARTBEAT_PERIOD);
            }
            localStateActor = this.stateActor;
            localSender = this.sender;
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
