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
import net.dovemq.transport.endpoint.EndpointTestUtils;

public class PerMessageTopicHierarchyTest {
    private static String brokerIP;
    private static int numIterations = 1000;

    private static String endpointName;
    private static int numSubscribers;

    private static final String[] topicHierarchies = new String[] {
      "root",
      "root.abc",
      "root.abc.ghi",
      "root.abc.ghi.stu",
      "root.abc.jkl",
      "root.def",
      "root.def.mno",
      "root.def.pqr",
      "root.def.pqr.vwxyz"
    };

    private static final String[] shutdownMessageHierarchies = new String[] {
        "root.abc.ghi.stu",
        "root.abc.jkl",
        "root.def.mno",
        "root.def.pqr.vwxyz"
      };

    private static final String[] publishTopicHierarchies = new String[] {
        "root.abc.ghi.stu",
        "root.abc.jkl",
        "other.abc.gh", // should not match
        "bar.def.pqrs", // should not match
        "root.def.pqr",
        "root.def.pqr.vwxyz",
        "mock.def.pqr.vwx", // should not match
        "root.def",
        "root.def.mno",
        "can.def.pqrst", // should not match
        "root.abc",
        "root.abc.ghi",
        "nook.abc.stu.ghi", // should not match
        "root",
        "root.abc.ghi.klmn",
        "root.def.xyz",
        "root.abc.ghi.stu.defghhk",
        "root.abc.jkl.mnop",
        "root.def.mno.pqr.stuv.wxy",
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

            String topicHierarchy = topicHierarchies[id];
            Subscriber subscriber = session.createHierarchicalTopicSubscriber(topicHierarchy);
            System.out.println("create subscriber: " + topicHierarchy);

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
        numIterations = Integer.parseInt(args[1]);
        endpointName = "TestPubSub";

        ConnectionFactory.initialize(endpointName);
        numSubscribers = topicHierarchies.length;
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
        Publisher publisher = session.createHierarchicalTopicPublisher("root");
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


        Random randomGenerator = new Random();
        for (int iter = 0; iter < numIterations; iter++) {

            for (String hierarchy : publishTopicHierarchies) {
                DoveMQMessage message = EndpointTestUtils.createEncodedMessage(randomGenerator, true);
                message.setTopicPublishHierarchy(hierarchy);
                publisher.publishMessage(message);
                messagesSent++;

                if (messagesSent%10000 == 0) {
                    System.out.println("Sent message count: " + messagesSent);
                }
            }
        }

        for (String hierarchy : shutdownMessageHierarchies) {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload("TOPIC_TEST_DONE".getBytes());
            message.setTopicPublishHierarchy(hierarchy);
            publisher.publishMessage(message);
            messagesSent++;
        }

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

        assertTrue(subscribers[0].numMessagesReceived() == 14*numIterations);
        assertTrue(subscribers[1].numMessagesReceived() == 7*numIterations);
        assertTrue(subscribers[2].numMessagesReceived() == 4*numIterations);
        assertTrue(subscribers[3].numMessagesReceived() == 2*numIterations);
        assertTrue(subscribers[4].numMessagesReceived() == 2*numIterations);
        assertTrue(subscribers[5].numMessagesReceived() == 6*numIterations);
        assertTrue(subscribers[6].numMessagesReceived() == 2*numIterations);
        assertTrue(subscribers[7].numMessagesReceived() == 2*numIterations);
        assertTrue(subscribers[8].numMessagesReceived() == 1*numIterations);

        Thread.sleep(2000);
        executor.shutdown();

        ConnectionFactory.shutdown();
    }
}