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

public class ProducerTest {
    private static final String SOURCE_NAME = "build.xml";

    private static String brokerIP;

    private static String endpointName;

    private static String queueNamePrefix;

    private static int numProducers;

    private static int numConsumersPerQueue = 1;

    private static int numIterations;

    private static boolean waitForAcks = true;

    private static class TestProducer extends CAMQPTestTask implements Runnable {
        public TestProducer(CountDownLatch startSignal,
                CountDownLatch doneSignal,
                int id) {
            super(startSignal, doneSignal);
            this.id = id;
        }

        @Override
        public void run() {
            waitForReady();
            try {
                Thread.sleep(new Random().nextInt(200) + 100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            session = ConnectionFactory.createSession(brokerIP);

            final AtomicInteger messageAckCount = new AtomicInteger(0);
            Producer producer = session.createProducer(String.format("%s.%d",
                    queueNamePrefix,
                    id));
            if (waitForAcks) {
                producer.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

                    @Override
                    public void messageAcknowledged(DoveMQMessage message) {
                        messageAckCount.incrementAndGet();
                    }
                });
            }

            int messagesSent = 0;
            try {
                for (int i = 0; i < numIterations; i++) {
                    messagesSent += sendFileContents(SOURCE_NAME, producer);
                }
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }

            /*
             * Send final message
             */
            for (int i = 0; i < numConsumersPerQueue; i++) {
                producer.sendMessage("QUEUE_TEST_DONE".getBytes());
                messagesSent++;
            }

            if (waitForAcks) {
                while (messageAckCount.get() < messagesSent) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("producer waiting: " + messagesSent
                                + " "
                                + messageAckCount.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                System.out.println("Producer sent " + messagesSent
                        + " messages");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            session.close();
            done();
        }

        private Session session;

        private final int id;
    }

    public static void main(String[] args) throws InterruptedException,
            IOException {
        brokerIP = args[0];
        endpointName = args[1];
        queueNamePrefix = args[2];
        numProducers = Integer.parseInt(args[3]);
        numConsumersPerQueue = Integer.parseInt(args[4]);
        numIterations = Integer.parseInt(args[5]);
        if (args.length > 6) {
            waitForAcks = Boolean.parseBoolean(args[6]);
        }

        ConnectionFactory.initialize(endpointName);

        ExecutorService executor = Executors.newFixedThreadPool(numProducers);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numProducers);

        for (int i = 0; i < numProducers; i++) {
            TestProducer producer = new TestProducer(startSignal, doneSignal, i);
            executor.submit(producer);
        }

        Thread.sleep(10000);
        startSignal.countDown();
        doneSignal.await();
        if (waitForAcks) {
            System.out.println("Producer got all messages acked");
        }
        executor.shutdown();

        ConnectionFactory.shutdown();
    }

    private static int sendFileContents(String fileName, Producer producer) throws IOException {
        int messageCount = 0;
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null) {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            producer.sendMessage(message);
            messageCount++;
        }
        freader.close();
        return messageCount;
    }
}
