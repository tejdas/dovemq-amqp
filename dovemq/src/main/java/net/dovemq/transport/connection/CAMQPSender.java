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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.jcip.annotations.ThreadSafe;

/**
 * Sender of AMQP frames. Owned by CAMQPConnection
 * @author tejdas
 *
 */
enum SenderState
{
    ACTIVE, CLOSE_REQUESTED, CLOSED
}

@ThreadSafe
class CAMQPSender implements ChannelFutureListener
{
    private final Channel channel;

    Channel getChannel()
    {
        return channel;
    }

    private final Queue<ChannelBuffer> queuedSends = new ConcurrentLinkedQueue<ChannelBuffer>();

    private final Queue<ChannelBuffer> queuedSendsConnectionControl = new ConcurrentLinkedQueue<ChannelBuffer>();

    private SenderState state = SenderState.ACTIVE;

    private boolean sendInProgress = false;

    private int outstandingWrites = 0;

    CAMQPSender(Channel channel)
    {
        super();
        this.channel = channel;
    }

    void close()
    {
        synchronized (this)
        {
            if (state != SenderState.ACTIVE)
            {
                return;
            }
            state = SenderState.CLOSE_REQUESTED;
            if (outstandingWrites > 0)
            {
                return;
            }
        }
        closeChannel();
    }

    synchronized void waitForClose()
    {
        try
        {
            while (state != SenderState.CLOSED)
            {
                wait();
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    synchronized boolean isClosed()
    {
        return (state == SenderState.CLOSED);
    }

    /**
     * When this method is called concurrently, one thread
     * assumes the role of sender and the other threads
     * just enqueue the outgoing frame.
     * 
     * Gives connection frames higher priority that session/link
     * frames.
     * 
     * @param data
     * @param frameType
     */
    void sendBuffer(ChannelBuffer data, int frameType)
    {
        synchronized (this)
        {
            if (state != SenderState.ACTIVE)
            {
                return;
            }
            if (sendInProgress)
            {
                if (frameType == CAMQPFrameConstants.FRAME_TYPE_CONNECTION)
                {
                    queuedSendsConnectionControl.offer(data);
                }
                else
                {
                    queuedSends.offer(data);
                }
                return;
            }
            sendInProgress = true;
            outstandingWrites++;
        }

        ChannelBuffer nextBuffer = data;
        while (true)
        {
            try
            {
                ChannelFuture future = channel.write(nextBuffer);
                future.addListener(this);
            }
            finally
            {
                synchronized (this)
                {
                    nextBuffer = queuedSendsConnectionControl.poll();
                    if (nextBuffer == null)
                    {
                        nextBuffer = queuedSends.poll();
                    }
                    if (nextBuffer == null)
                    {
                        sendInProgress = false;
                        return;
                    }
                    else
                    {
                        outstandingWrites++;
                    }
                }
            }
        }
    }

    @Override
    public void operationComplete(ChannelFuture future)
    {
        synchronized (this)
        {
            outstandingWrites--;
            if ((outstandingWrites > 0) || (state != SenderState.CLOSE_REQUESTED))
            {
                return;
            }
        }
        closeChannel();
    }

    private void closeChannel()
    {
        try
        {
            ChannelFuture future = channel.close();
            future.awaitUninterruptibly();
        }
        finally
        {
            synchronized (this)
            {
                queuedSendsConnectionControl.clear();
                queuedSends.clear();
                state = SenderState.CLOSED;
                notify();
            }
        }
    }
}
