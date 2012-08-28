package net.dovemq.transport.session;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public abstract class SessionTestTask
{
    public SessionTestTask(CountDownLatch startSignal, CountDownLatch doneSignal)
    {
        super();
        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
    }
    
    public void waitForReady()
    {
        Random r = new Random();
        try
        {
            startSignal.await();
            Thread.sleep(r.nextInt(100));
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void done()
    {
        doneSignal.countDown();
    }

    private final CountDownLatch startSignal; 
    private final CountDownLatch doneSignal;
}
