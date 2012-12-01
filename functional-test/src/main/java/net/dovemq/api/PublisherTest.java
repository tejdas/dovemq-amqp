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

public class PublisherTest
{
    private static String brokerIP;
    private static String endpointName;
    private static String topicName;
    private static String fileName;
    private static int numIterations;
    private static int NUM_THREADS = 1;

    private static class TestPublisher extends CAMQPTestTask implements Runnable
    {
        public TestPublisher(CountDownLatch startSignal, CountDownLatch doneSignal, Session session)
        {
            super(startSignal, doneSignal);
            //this.session = session;
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
            System.out.println("created session");

            Publisher publisher = session.createPublisher(topicName);

            final AtomicInteger messageAckCount = new AtomicInteger(0);
            publisher.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

                @Override
                public void messageAcknowledged(DoveMQMessage message)
                {
                    messageAckCount.incrementAndGet();
                }
            });

            System.out.println("created publisher");

            try
            {
                Thread.sleep(10000);
            }
            catch (InterruptedException e1)
            {
                Thread.currentThread().interrupt();
            }

            String sourceName = System.getenv("DOVEMQ_TEST_DIR") + "/" + fileName;
            int messagesSent = 0;
            for (int i = 0; i < numIterations; i++)
            {
                try
                {
                    messagesSent += sendFileContents(sourceName, publisher);
                }
                catch (IOException e)
                {
                    Thread.currentThread().interrupt();
                }
            }

            while (messageAckCount.get() < messagesSent)
            {
                try
                {
                    Thread.sleep(5000);
                    System.out.println("publisher waiting: " + messagesSent + " " + messageAckCount.get());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }

            done();
        }
        private Session session;
    }

    public static void main(String[] args) throws InterruptedException, IOException
    {
        brokerIP = args[0];
        endpointName = args[1];
        topicName = args[2];
        fileName = args[3];
        numIterations = Integer.parseInt(args[4]);
        NUM_THREADS = Integer.parseInt(args[5]);

        ConnectionFactory.initialize(endpointName);

        Session session = ConnectionFactory.createSession(brokerIP);
        System.out.println("created session");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++)
        {
            TestPublisher publisher = new TestPublisher(startSignal, doneSignal, session);
            executor.submit(publisher);
        }

        startSignal.countDown();
        doneSignal.await();

        /*
         * Send final message
         */
        Publisher publisher = session.createPublisher(topicName);

        final AtomicInteger messageAckCount = new AtomicInteger(0);
        publisher.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

            @Override
            public void messageAcknowledged(DoveMQMessage message)
            {
                messageAckCount.incrementAndGet();
            }
        });

        System.out.println("created publisher");

        int messagesSent = 0;
        publisher.publishMessage("TOPIC_TEST_DONE".getBytes());
        messagesSent++;

        while (messageAckCount.get() < messagesSent)
        {
            try
            {
                Thread.sleep(5000);
                System.out.println("publisher waiting: " + messagesSent + " " + messageAckCount.get());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        Thread.sleep(2000);
        session.close();
        executor.shutdown();

        ConnectionFactory.shutdown();
    }

    private static int sendFileContents(String fileName, Publisher publisher) throws IOException
    {
        int messageCount = 0;
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            publisher.publishMessage(message);
            messageCount++;
        }
        freader.close();
        return messageCount;
    }
}
