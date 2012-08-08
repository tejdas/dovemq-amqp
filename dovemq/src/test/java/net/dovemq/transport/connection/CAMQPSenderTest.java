package net.dovemq.transport.connection;

import java.util.concurrent.CountDownLatch;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import net.dovemq.transport.connection.mockjetty.MockJettyChannel;
import net.dovemq.transport.frame.CAMQPFrameConstants;

import junit.framework.TestCase;

public class CAMQPSenderTest extends TestCase
{
    private static final int numThreads = 10;
    final static int numMessages = 100;    
    final CountDownLatch startGate = new CountDownLatch(1);
    final CountDownLatch endGate = new CountDownLatch(numThreads);

    public CAMQPSenderTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void
    testCAMQPSenderNoMsg()
    {
        Channel channel = new MockJettyChannel(true);
        CAMQPSender sender = new CAMQPSender(channel);
        assertTrue(sender.getChannel() == channel);        
        sender.close();
        sender.waitForClose();
        assertTrue(sender.isClosed());       
    }
    
    public void
    testCAMQPSender()
    {
        Channel channel = new MockJettyChannel(true);
        CAMQPSender sender = new CAMQPSender(channel);
        assertTrue(sender.getChannel() == channel);        
        for (int i = 0; i < numMessages; i++)
        {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(128);
            sender.sendBuffer(buffer, CAMQPFrameConstants.FRAME_TYPE_CONNECTION);
        }
        sender.close();
        assertFalse(sender.isClosed());        
        sender.waitForClose();
        assertTrue(sender.isClosed());
        assertEquals(((MockJettyChannel) channel).getNumberOfWrites(), 100);        
    }    
    
    static class CAMQPSenderThread implements Runnable
    {
        protected CAMQPSenderThread(CAMQPSender sender, int frequencyOfFrameTypeConnection, CAMQPSenderTest parent)
        {
            super();
            this.sender = sender;
            this.frequencyOfFrameTypeConnection = frequencyOfFrameTypeConnection;
            this.parent = parent;
        }

        private final CAMQPSender sender;
        private final int frequencyOfFrameTypeConnection;
        private final CAMQPSenderTest parent; 
        
        @Override
        public void run()
        {
            try
            {
                parent.startGate.await();
            }
            catch (InterruptedException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            try
            {
                for (int i = 0; i < CAMQPSenderTest.numMessages; i++)
                {
                    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(128);
                    if (i%frequencyOfFrameTypeConnection == 0)
                    {
                        sender.sendBuffer(buffer, CAMQPFrameConstants.FRAME_TYPE_CONNECTION);                        
                    }
                    else
                    {
                        sender.sendBuffer(buffer, CAMQPFrameConstants.FRAME_TYPE_SESSION);
                    }
                }                
            }
            finally
            {
                parent.endGate.countDown();                
            }
        }        
    }

    public void
    testCAMQPSenderMT()
    {
        Channel channel = new MockJettyChannel(true);
        CAMQPSender sender = new CAMQPSender(channel);
        assertTrue(sender.getChannel() == channel);
        
        for (int i = 0; i < numThreads; i++)
        {
            new Thread(new CAMQPSenderThread(sender, 5+i, this)).start();
        }
        startGate.countDown();
        try
        {
            endGate.await();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sender.close();
        sender.waitForClose();
        assertTrue(sender.isClosed());
        assertEquals(((MockJettyChannel) channel).getNumberOfWrites(), numMessages*numThreads);
    }    
}

