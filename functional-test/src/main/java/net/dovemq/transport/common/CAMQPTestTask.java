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

package net.dovemq.transport.common;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public abstract class CAMQPTestTask
{
    public CAMQPTestTask(CountDownLatch startSignal, CountDownLatch doneSignal)
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
