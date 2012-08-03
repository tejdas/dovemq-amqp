package net.dovemq.transport.connection;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import net.dovemq.transport.frame.CAMQPFrameConstants;

enum SenderState
{
    ACTIVE, CLOSE_REQUESTED, CLOSED
}

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
                wait();
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
    public void operationComplete(ChannelFuture future) throws Exception
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
