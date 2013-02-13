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

package net.dovemq.api;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.protocol.data.CAMQPConstants;

public class ProducerAlreadyConnectedTest extends TestCase
{
    private static String brokerIP;
    private static Session session = null;
    private static final CountDownLatch producerCreatedSignal = new CountDownLatch(1);

    private static class TestProducer extends CAMQPTestTask implements Runnable
    {
        public TestProducer(CountDownLatch startSignal, CountDownLatch doneSignal)
        {
            super(startSignal, doneSignal);
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(new Random().nextInt(200) + 100);
            }
            catch (InterruptedException e)
            {
            }

            session.createProducer("testQueue");
            producerCreatedSignal.countDown();
            waitForReady();
            done();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException
    {
        brokerIP = args[0];

        ConnectionFactory.initialize("foobar");
        session = ConnectionFactory.createSession(brokerIP);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);

        TestProducer producer = new TestProducer(startSignal, doneSignal);
        executor.submit(producer);

        producerCreatedSignal.await();

        Producer anotherProducer = session.createProducer("testQueue");
        Thread.sleep(5000);
        try {
            anotherProducer.sendMessage("should not go through".getBytes());
            assertFalse("Should have thrown RuntimeException", true);
        } catch (RuntimeException ex) {
            System.out.println("Caught RuntimeException: " + ex.toString());
            assertTrue(ex.toString().contains(CAMQPConstants.AMQP_ERROR_RESOURCE_LOCKED));
            assertTrue(ex.toString(), true);
        }

        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        session.close();
        ConnectionFactory.shutdown();
    }
}
