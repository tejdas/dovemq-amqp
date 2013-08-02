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

import static junit.framework.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.dovemq.transport.common.CAMQPTestTask;

public class TopicTagFilterTest {
    private static String brokerIP;
    private static String endpointName;
    private static int numSubscribers;

    private static final String topicName = "TagFilterRoot";

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private static final String EMAIL_ADDRESS_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*" +
            "@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final String[] subscriberPatterns = new String[] {
        IPADDRESS_PATTERN,
        EMAIL_ADDRESS_PATTERN
    };

    private static final String[] publishMessageTags = new String[] {
        "192.168.1.105",
        "abc@xyz.com",
        "abc@.com", // should not match
        "168.242.91.37",
        "278.34.12.456", // should not match
        "xyz-123@pqr.in",
        "192.255.127" // should not match
    };

    private static class TestMessageReceiver implements DoveMQMessageReceiver {
        volatile boolean shutdown = false;
        protected final AtomicInteger messageReceivedCount = new AtomicInteger(0);

        public TestMessageReceiver() {
            super();
        }

        @Override
        public void messageReceived(DoveMQMessage message) {

            byte[] body = message.getPayload();
            String bString = new String(body);

            if ("TOPIC_TEST_DONE".equalsIgnoreCase(bString)) {
                shutdown = true;
            } else {
                messageReceivedCount.incrementAndGet();
            }
        }
    }

    private static class TestSubscriber extends CAMQPTestTask implements
            Runnable {
        public TestSubscriber(CountDownLatch startSignal,
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
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


            Connection connection = ConnectionFactory.createConnection(brokerIP);
            session = connection.createSession();

            Subscriber subscriber = session.createTagFilterSubscriber(topicName, subscriberPatterns[id]);
            System.out.println("create subscriber: " + topicName);

            messageReceiver = new TestMessageReceiver();
            subscriber.registerMessageReceiver(messageReceiver);

            while (!messageReceiver.shutdown) {
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            done();
        }

        public int numMessagesReceived() {
            return messageReceiver.messageReceivedCount.intValue();
        }

        private Session session;

        private final int id;
        private TestMessageReceiver messageReceiver;
    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        brokerIP = args[0];
        endpointName = "TestPubSub";

        ConnectionFactory.initialize(endpointName);
        numSubscribers = subscriberPatterns.length;
        ExecutorService executor = Executors.newFixedThreadPool(numSubscribers);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numSubscribers);

        TestSubscriber[] subscribers = new TestSubscriber[numSubscribers];
        for (int i = 0; i < numSubscribers; i++) {
            TestSubscriber subscriber = new TestSubscriber(startSignal, doneSignal, i);
            subscribers[i] = subscriber;
            executor.submit(subscriber);
        }

        startSignal.countDown();

        Thread.sleep(10000);

        Session session = ConnectionFactory.createSession(brokerIP);
        Publisher publisher = session.createTagFilterPublisher(topicName);
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

        int numIterations = 1000;
        for (int iter = 0; iter < numIterations; iter++) {

            for (String tag : publishMessageTags) {
                DoveMQMessage message = MessageFactory.createMessage();
                message.addPayload(tag.getBytes());
                message.setRoutingTag(tag);
                publisher.publishMessage(message);
                messagesSent++;
            }
        }

        /*
         * Send shut-down message
         */
        DoveMQMessage shutdownMessage = MessageFactory.createMessage();
        shutdownMessage.addPayload("TOPIC_TEST_DONE".getBytes());
        shutdownMessage.setRoutingTag("255.255.255.0");
        publisher.publishMessage(shutdownMessage);
        messagesSent++;

        DoveMQMessage shutdownMessage2 = MessageFactory.createMessage();
        shutdownMessage2.addPayload("TOPIC_TEST_DONE".getBytes());
        shutdownMessage2.setRoutingTag("abc@xyz.in");
        publisher.publishMessage(shutdownMessage2);
        messagesSent++;

        while (messageAckCount.get() < messagesSent) {
            try {
                Thread.sleep(5000);
                System.out.println("publisher waiting: " + messagesSent
                        + " "
                        + messageAckCount.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        doneSignal.await();

        System.out.println("Num messages received: " + subscribers[0].numMessagesReceived());
        System.out.println("Num messages received: " + subscribers[1].numMessagesReceived());

        assertTrue(subscribers[0].numMessagesReceived() == 2*numIterations);
        assertTrue(subscribers[1].numMessagesReceived() == 2*numIterations);

        Thread.sleep(2000);
        executor.shutdown();

        ConnectionFactory.shutdown();
    }
}