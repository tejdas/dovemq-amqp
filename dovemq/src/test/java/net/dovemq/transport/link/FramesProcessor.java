package net.dovemq.transport.link;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

abstract class BaseMessageSender implements Runnable
{
    public BaseMessageSender(int numMessagesToSend, boolean pauseBetweenSends)
    {
        super();
        this.numMessagesToSend = numMessagesToSend;
        this.pauseBetweenSends = pauseBetweenSends;
    }

    private final int numMessagesToSend;
    private final boolean pauseBetweenSends;
    private volatile boolean shutdown = false;
    private final CountDownLatch doneSignal = new CountDownLatch(1);
    
    void waitForDone() throws InterruptedException
    {
        shutdown = true;
        doneSignal.await();
    }
    
    abstract void doSend();
    
    @Override
    public void run()
    {
        Random r = new Random();
        
        for (int i = 0; i < numMessagesToSend; i++)
        {
            if (shutdown)
            {
                break;
            }
            
            doSend();
            
            if (pauseBetweenSends)
            {
                try
                {
                    Thread.sleep(r.nextInt(25));
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
        doneSignal.countDown();
    } 
}

class FramesProcessor implements Runnable
{
    public AtomicLong linkFlowFrameCount = new AtomicLong(0L);
    
    private final BlockingQueue<ChannelBuffer> framesQueue;
    private final BlockingQueue<CAMQPControlTransfer> transferFramesQueue;
    private final BlockingQueue<Object> controlFramesQueue;
    
    public FramesProcessor(BlockingQueue<ChannelBuffer> framesQueue, BlockingQueue<CAMQPControlTransfer> transferFramesQueue, BlockingQueue<Object> controlFramesQueue)
    {
        super();
        this.framesQueue = framesQueue;
        this.transferFramesQueue = transferFramesQueue;
        this.controlFramesQueue = controlFramesQueue;
    }
    
    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                ChannelBuffer buffer = framesQueue.take();
                if (buffer == null)
                {
                    break;
                }
     
                CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
                inputPipe.take(buffer);
            
                String controlName = inputPipe.readSymbol();
                if (controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor))
                {
                    transferFramesQueue.put(CAMQPControlTransfer.decode(inputPipe));   
                }
                else
                {
                    Object controlData = null;
                    if (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor))
                    {
                        controlData = CAMQPControlFlow.decode(inputPipe);

                        if (((CAMQPControlFlow) controlData).isSetHandle())
                        {
                            linkFlowFrameCount.incrementAndGet();
                            controlFramesQueue.put(controlData);
                        }
                    }
                    else if (controlName.equalsIgnoreCase(CAMQPControlAttach.descriptor))
                    {
                        controlData = CAMQPControlAttach.decode(inputPipe);
                        controlFramesQueue.put(controlData);
                    }
                    else if (controlName.equalsIgnoreCase(CAMQPControlDetach.descriptor))
                    {
                        controlData = CAMQPControlDetach.decode(inputPipe);
                        controlFramesQueue.put(controlData);
                    }
                }
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            framesQueue.clear();
        } 
    }
}