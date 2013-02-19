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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.endpoint.EndpointTestUtils;

public class TopicHierarchyTest extends TestCase {
    private static String brokerIP;
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
        "root.def.pqr",
        "root.def.pqr.vwxyz",
        "root.def",
        "root.def.mno",
        "root.abc",
        "root",
        "root.abc.ghi",
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

        Publisher[] publishers = new Publisher[publishTopicHierarchies.length];

        for (int i = 0; i < publishTopicHierarchies.length; i++) {
            String hierarchy = publishTopicHierarchies[i];
            Publisher pub = session.createHierarchicalTopicPublisher(hierarchy);
            publishers[i] = pub;
            pub.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

                @Override
                public void messageAcknowledged(DoveMQMessage message)
                {
                    messageAckCount.incrementAndGet();
                }
            });
            System.out.println("created publisher: " + hierarchy);
        }

        Random randomGenerator = new Random();
        int numIterations = 100000;

        for (int i = 0; i < publishTopicHierarchies.length; i++) {
            Publisher pub = publishers[i];
            for (int iter = 0; iter < numIterations; iter++) {
                DoveMQMessage message = EndpointTestUtils.createEncodedMessage(randomGenerator, true);
                pub.publishMessage(message);
                messagesSent++;
            }
        }

        for (String hierarchy : shutdownMessageHierarchies) {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload("TOPIC_TEST_DONE".getBytes());
            message.setTopicPublishHierarchy(hierarchy);
            publisher.publishMessage(message);
            messagesSent++;
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