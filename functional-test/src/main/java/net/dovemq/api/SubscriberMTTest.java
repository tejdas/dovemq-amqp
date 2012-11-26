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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.dovemq.transport.common.CAMQPTestTask;

public class SubscriberMTTest
{
    private static String brokerIP;
    private static String endpointName;
    private static String topicName;
    private static int NUM_THREADS;
    private static boolean shareSession;

    private static class TestMessageReceiver implements DoveMQMessageReceiver
    {
        volatile boolean shutdown = false;
        public TestMessageReceiver(PrintWriter fw)
        {
            super();
            this.fw = fw;
        }

        @Override
        public void messageReceived(DoveMQMessage message)
        {
            byte[] body = message.getPayload();
            String bString = new String(body);

            if ("TOPIC_TEST_DONE".equalsIgnoreCase(bString))
            {
                shutdown = true;
            }
            else
            {
                fw.println(bString);
            }
        }

        private final PrintWriter fw;
    }

    private static class TestSubscriber extends CAMQPTestTask implements Runnable
    {
        public TestSubscriber(CountDownLatch startSignal, CountDownLatch doneSignal, Session session, int id)
        {
            super(startSignal, doneSignal);
            this.session = session;
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

            if (session == null)
                session = ConnectionFactory.createSession(brokerIP);

            Subscriber subscriber = session.createSubscriber(topicName);

            String fileName = String.format("%s-%d.txt", endpointName, id);
            PrintWriter fw = null;
            try
            {
                fw = new PrintWriter(fileName);
            }
            catch (FileNotFoundException e)
            {
            }

            TestMessageReceiver messageReceiver = new TestMessageReceiver(fw);
            subscriber.registerMessageReceiver(messageReceiver);

            while (!messageReceiver.shutdown)
            {
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                }
            }

            fw.flush();
            fw.close();
            done();
        }
        private Session session;
        private final int id;
    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException
    {
        brokerIP = args[0];
        endpointName = args[1];
        topicName = args[2];
        NUM_THREADS = Integer.parseInt(args[3]);
        shareSession = Boolean.parseBoolean(args[4]);

        ConnectionFactory.initialize(endpointName);

        Session session = null;
        if (shareSession)
            session = ConnectionFactory.createSession(brokerIP);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++)
        {
            TestSubscriber subscriber = new TestSubscriber(startSignal, doneSignal, session, i);
            executor.submit(subscriber);
        }

        startSignal.countDown();
        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();

        ConnectionFactory.shutdown();
    }
}
