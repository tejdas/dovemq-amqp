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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.dovemq.transport.common.CAMQPTestTask;

public class ProducerTest
{
    private static String brokerIP;
    private static String endpointName;
    private static String queueName;
    private static int NUM_THREADS;
    private static int numIterations;

    private static class TestProducer extends CAMQPTestTask implements Runnable
    {
        public TestProducer(CountDownLatch startSignal, CountDownLatch doneSignal, int id)
        {
            super(startSignal, doneSignal);
            this.id = id;
        }

        @Override
        public void run()
        {
            waitForReady();
            try
            {
                Thread.sleep(new Random().nextInt(200) + 100);
            }
            catch (InterruptedException e)
            {
            }

            DoveMQEndpointPolicy policy = new DoveMQEndpointPolicy();
            policy.createEndpointOnNewConnection();
            session = ConnectionFactory.createSession(brokerIP, policy);

            final AtomicInteger messageAckCount = new AtomicInteger(0);
            Producer producer = session.createProducer(String.format("%s.%d", queueName, id));
            producer.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

                @Override
                public void messageAcknowledged(DoveMQMessage message)
                {
                    messageAckCount.incrementAndGet();
                }
            });

            String sourceName = System.getenv("DOVEMQ_TEST_DIR") + "/build.xml";

            int messagesSent = 0;
            try
            {
                for (int i = 0; i < numIterations; i++)
                {
                    messagesSent += sendFileContents(sourceName, producer);
                }
            }
            catch (IOException e)
            {
                Thread.currentThread().interrupt();
            }

            /*
             * Send final message
             */
            producer.sendMessage("QUEUE_TEST_DONE".getBytes());
            messagesSent++;

            while (messageAckCount.get() < messagesSent)
            {
                try
                {
                    Thread.sleep(5000);
                    System.out.println("producer waiting: " + messagesSent + " " + messageAckCount.get());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }

            done();
        }
        private Session session;
        private final int id;

    }

    public static void main(String[] args) throws InterruptedException, IOException
    {
        brokerIP = args[0];
        endpointName = args[1];
        queueName = args[2];
        NUM_THREADS = Integer.parseInt(args[3]);
        numIterations = Integer.parseInt(args[4]);

        ConnectionFactory.initialize(endpointName);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++)
        {
            TestProducer producer = new TestProducer(startSignal, doneSignal, i);
            executor.submit(producer);
        }

        Thread.sleep(10000);
        startSignal.countDown();
        doneSignal.await();
        System.out.println("Producer got all messages acked");
        executor.shutdown();

        ConnectionFactory.shutdown();
    }

    private static int sendFileContents(String fileName, Producer producer) throws IOException
    {
        int messageCount = 0;
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            producer.sendMessage(message);
            messageCount++;
        }
        freader.close();
        return messageCount;
    }
}
