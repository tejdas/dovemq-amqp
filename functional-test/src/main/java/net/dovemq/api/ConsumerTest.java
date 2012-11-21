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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.transport.common.CAMQPTestTask;

public class ConsumerTest
{
    private static String brokerIP;
    private static String endpointName;
    private static String queueName;
    private static int NUM_THREADS;
    private static boolean ackExplicit = false;
    private static int sleepSeconds;
    private static ExecutorService executor;

    private static class TestMessageReceiver implements DoveMQMessageReceiver
    {
        private final BlockingQueue<DoveMQMessage> receivedMessages = new LinkedBlockingQueue<DoveMQMessage>();

        public TestMessageReceiver(PrintWriter fw, Consumer consumer)
        {
            super();
            this.fw = fw;
            this.consumer = consumer;
        }

        @Override
        public void messageReceived(DoveMQMessage message)
        {
            Collection<byte[]> body = message.getPayloads();
            for (byte[] b : body)
            {
                String bString = new String(b);
                fw.println(bString);

                if (ackExplicit)
                    receivedMessages.add(message);
            }
        }

        void ackMessage()
        {
            DoveMQMessage message = receivedMessages.poll();
            try
            {
                Thread.sleep(new Random().nextInt(25) + 25);
            }
            catch (InterruptedException e)
            {
            }
            if (message != null)
            {
                consumer.acknowledge(message);
            }
        }

        private final PrintWriter fw;
        private final Consumer consumer;
    }

    private static class MessageAcker implements Runnable
    {
        public MessageAcker(TestMessageReceiver messageReceiver)
        {
            super();
            this.messageReceiver = messageReceiver;
        }

        @Override
        public void run()
        {
            while (!done)
            {
                messageReceiver.ackMessage();
            }
        }

        public void shutdown()
        {
            done = true;
        }

        private final TestMessageReceiver messageReceiver;
        private volatile boolean done = false;
    }

    private static class TestConsumer extends CAMQPTestTask implements Runnable
    {
        public TestConsumer(CountDownLatch startSignal, CountDownLatch doneSignal, Session session, int id)
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
            {
                DoveMQEndpointPolicy policy = new DoveMQEndpointPolicy();
                policy.createEndpointOnNewConnection();
                session = ConnectionFactory.createSession(brokerIP, policy);
            }

            String suffixedQueueName = String.format("%s.%d", queueName, id);
            Consumer consumer = null;

            if (ackExplicit)
                consumer = session.createConsumer(suffixedQueueName, new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS));
            else
                consumer = session.createConsumer(suffixedQueueName);

            String fileName = String.format("%s-%d.txt", endpointName, id);
            PrintWriter fw = null;
            try
            {
                fw = new PrintWriter(fileName);
            }
            catch (FileNotFoundException e)
            {
            }

            TestMessageReceiver messageReceiver = new TestMessageReceiver(fw, consumer);
            consumer.registerMessageReceiver(messageReceiver);
            MessageAcker acker = new MessageAcker(messageReceiver);

            if (ackExplicit)
                executor.submit(acker);

            System.out.println("consumer sleeping for " + sleepSeconds + " seconds");
            try
            {
                Thread.sleep(1000*sleepSeconds);
            }
            catch (InterruptedException e)
            {
            }

            if (ackExplicit)
                acker.shutdown();

            fw.flush();
            fw.close();
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
        ackExplicit = Boolean.parseBoolean(args[4]);
        sleepSeconds = Integer.parseInt(args[5]);

        ConnectionFactory.initialize(endpointName);

        Session session = null;

        executor = Executors.newFixedThreadPool(NUM_THREADS*2);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++)
        {
            TestConsumer consumer = new TestConsumer(startSignal, doneSignal, session, i);
            executor.submit(consumer);
        }

        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        ConnectionFactory.shutdown();
    }
}
