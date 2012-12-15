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

package net.dovemq.transport.connection.mockjetty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;

public class MockJettyChannel implements org.jboss.netty.channel.Channel
{
    private final AtomicInteger numberOfWrites = new AtomicInteger(0);
    public int
    getNumberOfWrites()
    {
        return numberOfWrites.get();
    }

    private final BlockingQueue<ChannelBuffer> sentBufferQueue = new LinkedBlockingQueue<ChannelBuffer>();
    public ChannelBuffer
    getNextBuffer()
    {
        try
        {
            return sentBufferQueue.take();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private final BlockingQueue<ChannelFuture> channelFutureQueue = new LinkedBlockingQueue<ChannelFuture>();

    static class MockWriteCompletionHandler implements Runnable
    {
        private final BlockingQueue<ChannelFuture> channelFutureQueue;
        MockWriteCompletionHandler(BlockingQueue<ChannelFuture> channelFutureQueue)
        {
            this.channelFutureQueue = channelFutureQueue;
        }
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    MockChannelFuture future = (MockChannelFuture) channelFutureQueue.take();
                    Thread.sleep(10);
                    ChannelFutureListener listener = future.getListener();
                    if (future.isLastFuture())
                    {
                        return;
                    }
                    listener.operationComplete(future);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public MockJettyChannel(boolean senderTest)
    {
        if (senderTest)
        {
            new Thread(new MockWriteCompletionHandler(channelFutureQueue)).start();
        }
    }

    @Override
    public ChannelFuture bind(SocketAddress arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture close()
    {
        // TODO Auto-generated method stub
        MockChannelFuture future = new MockChannelFuture();
        future.setLastFuture(true);
        try
        {
            channelFutureQueue.put(future);
            return future;
        }
        catch (InterruptedException e)
        {
            return null;
        }
        finally
        {
            sentBufferQueue.clear();
        }
    }

    @Override
    public ChannelFuture connect(SocketAddress arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture disconnect()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture getCloseFuture()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelConfig getConfig()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFactory getFactory()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getInterestOps()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public SocketAddress getLocalAddress()
    {
        return InetSocketAddress.createUnresolved("localhost", 8746);
    }

    @Override
    public Channel getParent()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelPipeline getPipeline()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress()
    {
        return InetSocketAddress.createUnresolved("localhost", 8746);
    }

    @Override
    public boolean isBound()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConnected()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOpen()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReadable()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWritable()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ChannelFuture setInterestOps(int arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture setReadable(boolean arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture unbind()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture write(Object arg0)
    {
        numberOfWrites.incrementAndGet();

        if (arg0 instanceof ChannelBuffer)
        {
            sentBufferQueue.add((ChannelBuffer) arg0);
        }
        ChannelFuture future = new MockChannelFuture();
        try
        {
            channelFutureQueue.put(future);
            return future;
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ChannelFuture write(Object arg0, SocketAddress arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int compareTo(Channel arg0)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getAttachment()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttachment(Object arg0)
    {
        // TODO Auto-generated method stub

    }

}
