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

package net.dovemq.broker.endpoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.endpoint.DoveMQMessageImpl;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueueRouterTest {
    private static final AtomicLong linkIds = new AtomicLong(0L);

    private static final AtomicLong deliveryIds = new AtomicLong(0L);

    enum TaskAction {
        ATTACH, DETACH, SEND_MESSAGE, ACK_MESSAGE, CHECK_RECEIVED_MESSAGE_COUNT, CHECK_RECEIVED_ACK_COUNT, SHUTDOWN
    }

    private static class Task {
        public Task(TaskAction action, int count) {
            super();
            this.action = action;
            this.count = count;
        }

        public Task(TaskAction action) {
            super();
            this.action = action;
            this.count = 0;
        }

        public final TaskAction action;

        public final int count;
    }

    private static class MockConsumerProxy implements CAMQPSourceInterface {
        private final long id = linkIds.getAndIncrement();

        public MockConsumerProxy(boolean delayedAck) {
            super();
            this.delayedAck = delayedAck;
        }

        public MockConsumerProxy() {
            super();
            this.delayedAck = false;
        }

        private final AtomicInteger messageCount = new AtomicInteger(0);

        private final AtomicInteger registrationCount = new AtomicInteger(0);

        private volatile CAMQPMessageDispositionObserver observer = null;

        private final BlockingQueue<DoveMQMessage> receivedMessages = new LinkedBlockingQueue<DoveMQMessage>();

        private final boolean delayedAck;

        public int getCount() {
            return messageCount.get();
        }

        public void waitForMessages(int count) {
            while (messageCount.get() < count) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public int getRegistrationCount() {
            return registrationCount.get();
        }

        @Override
        public void registerDispositionObserver(CAMQPMessageDispositionObserver observer) {
            this.observer = observer;
            registrationCount.incrementAndGet();
        }

        @Override
        public void sendMessage(DoveMQMessage message) {
            messageCount.incrementAndGet();
            if (delayedAck) {
                receivedMessages.add(message);
            }
            else {
                observer.messageAckedByConsumer(message, this);
            }
        }

        @Override
        public CAMQPMessage getMessage() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getMessageCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void messageSent(long deliveryId, CAMQPMessage message) {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState) {
            // TODO Auto-generated method stub
            return null;
        }

        public void ackMessages(int count) {
            int ackedCount = 0;
            for (int i = 0; i < count; i++) {
                DoveMQMessage message = receivedMessages.poll();
                if (message != null) {
                    observer.messageAckedByConsumer(message, this);
                    ackedCount++;
                }
                else
                    break;
            }

            if (ackedCount < count) {
                assertFalse("wanted to ack " + count + " messages, but found only " + ackedCount + " messages", true);
            }
        }

        @Override
        public long getId() {
            return id;
        }
    }

    private static class MockProducerSink implements CAMQPTargetInterface {
        private final long id = linkIds.getAndIncrement();

        private final AtomicInteger registrationCount = new AtomicInteger(0);

        private final AtomicInteger ackedMessageCount = new AtomicInteger(0);

        public int getRegistrationCount() {
            return registrationCount.get();
        }

        public int getAckedMessageCount() {
            return ackedMessageCount.get();
        }

        public void waitForAcks(int count) {
            while (ackedMessageCount.get() < count) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        @Override
        public void registerMessageReceiver(CAMQPMessageReceiver messageReceiver) {
            registrationCount.incrementAndGet();
        }

        @Override
        public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode) {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void acknowledgeMessageProcessingComplete(long deliveryId) {
            ackedMessageCount.incrementAndGet();
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public void closeUnderlyingLink(CAMQPDefinitionError errorDetails) {
            // TODO Auto-generated method stub
        }
    }

    private abstract static class TaskRunner implements Runnable {
        private volatile boolean isDone = false;

        private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>();

        protected final QueueRouter queueRouter;

        public void submitTask(TaskAction taskAction) {
            taskQueue.add(new Task(taskAction));
        }

        public void submitTask(TaskAction taskAction, int messageCount) {
            taskQueue.add(new Task(taskAction, messageCount));
        }

        public void waitUntilDone() {
            while (!isDone) {
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public TaskRunner(QueueRouter queueRouter) {
            super();
            this.queueRouter = queueRouter;
        }

        public abstract void executeTask(Task task);

        @Override
        public void run() {
            while (true) {
                try {
                    Task task = taskQueue.take();
                    if (task.action == TaskAction.SHUTDOWN) {
                        isDone = true;
                        break;
                    }
                    else {
                        executeTask(task);
                    }

                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

            }
        }
    }

    private static class TestProducer extends TaskRunner {
        private final MockProducerSink producerSink = new MockProducerSink();

        public TestProducer(QueueRouter queueRouter) {
            super(queueRouter);
        }

        @Override
        public void executeTask(Task task) {
            switch (task.action) {
            case ATTACH:
                queueRouter.producerAttached(producerSink);
                break;

            case DETACH:
                queueRouter.producerDetached(producerSink);
                break;

            case SEND_MESSAGE:
                QueueRouterTest.sendMessages(task.count, queueRouter, producerSink);
                break;

            case CHECK_RECEIVED_ACK_COUNT:
                producerSink.waitForAcks(task.count);
                break;
            }
        }

        void waitForAcks(int count) {
            producerSink.waitForAcks(count);
        }
    }

    private static class TestConsumer extends TaskRunner {
        private final MockConsumerProxy consumerProxy = new MockConsumerProxy(true);

        public TestConsumer(QueueRouter queueRouter) {
            super(queueRouter);
        }

        @Override
        public void executeTask(Task task) {
            switch (task.action) {
            case ATTACH:
                queueRouter.consumerAttached(consumerProxy);
                break;

            case DETACH:
                queueRouter.consumerDetached(consumerProxy);
                break;

            case ACK_MESSAGE:
                consumerProxy.ackMessages(task.count);
                break;

            case CHECK_RECEIVED_MESSAGE_COUNT:
                consumerProxy.waitForMessages(task.count);
                break;
            }
        }
    }

    @BeforeClass
    public static void setupBeforeClass() throws Exception
    {
        DoveMQEndpointDriver.setManager(new DoveMQEndpointManagerImpl());
    }

    @Before
    public void setup() {

    }

    @After
    public void tearDown() {

    }

    @AfterClass
    public static void teardownAfterClass() throws Exception
    {
        DoveMQEndpointDriver.shutdownManager();
    }

    @Test
    public void testBasic() {
        QueueRouter queueRouter = new QueueRouter("test");

        MockConsumerProxy consumer1 = new MockConsumerProxy();
        MockConsumerProxy consumer2 = new MockConsumerProxy();
        MockConsumerProxy consumer3 = new MockConsumerProxy();

        queueRouter.consumerAttached(consumer1);
        queueRouter.consumerAttached(consumer2);
        queueRouter.consumerAttached(consumer3);

        MockProducerSink producer = new MockProducerSink();
        queueRouter.producerAttached(producer);

        /*
         * Register one more time, and assert that it doesn't get registered
         * again.
         */
        queueRouter.consumerAttached(consumer2);
        queueRouter.consumerAttached(consumer3);
        queueRouter.producerAttached(producer);
        assertTrue(1 == consumer2.getRegistrationCount());
        assertTrue(1 == consumer3.getRegistrationCount());
        assertTrue(1 == producer.getRegistrationCount());

        /*
         * Assert that a second producer is not attached until the first
         * producer is detached.
         */
        MockProducerSink producer2 = new MockProducerSink();
        queueRouter.producerAttached(producer2);
        assertTrue(0 == producer2.getRegistrationCount());

        queueRouter.producerDetached(producer);
        queueRouter.producerAttached(producer2);
        assertTrue(1 == producer2.getRegistrationCount());

        int messagesPerConsumer = 100;
        for (int i = 0; i < 3 * messagesPerConsumer; i++) {
            DoveMQMessage message = MessageFactory.createMessage();
            DoveMQMessageImpl messageImpl = ((DoveMQMessageImpl) message);
            messageImpl.setDeliveryId(i);
            queueRouter.messageReceived(message, producer2);
        }

        assertTrue(consumer1.getCount() == messagesPerConsumer);
        assertTrue(consumer2.getCount() == messagesPerConsumer);
        assertTrue(consumer3.getCount() == messagesPerConsumer);
        assertTrue(producer2.getAckedMessageCount() == 3 * messagesPerConsumer);

        queueRouter.producerDetached(producer2);
        queueRouter.consumerDetached(consumer1);
        queueRouter.consumerDetached(consumer2);
        queueRouter.consumerDetached(consumer3);
        assertTrue(queueRouter.isCompletelyDetached());
    }

    @Test
    public void testProducerAttachedBeforeConsumer() {
        QueueRouter queueRouter = new QueueRouter("test");

        MockProducerSink producer = new MockProducerSink();
        queueRouter.producerAttached(producer);

        int messagesPerConsumer = 100;
        for (int i = 0; i < messagesPerConsumer; i++) {
            DoveMQMessage message = MessageFactory.createMessage();
            queueRouter.messageReceived(message, producer);
        }

        MockConsumerProxy consumer = new MockConsumerProxy();
        queueRouter.consumerAttached(consumer);

        consumer.waitForMessages(messagesPerConsumer);
        assertTrue(consumer.getCount() == messagesPerConsumer);
    }

    @Test
    public void testProducerAttachedAndDetachedBeforeConsumer() {
        QueueRouter queueRouter = new QueueRouter("test");

        MockProducerSink producer = new MockProducerSink();
        queueRouter.producerAttached(producer);

        int messagesPerConsumer = 100;
        for (int i = 0; i < messagesPerConsumer; i++) {
            DoveMQMessage message = MessageFactory.createMessage();
            queueRouter.messageReceived(message, producer);
        }

        queueRouter.producerDetached(producer);
        MockConsumerProxy consumer = new MockConsumerProxy();
        queueRouter.consumerAttached(consumer);

        consumer.waitForMessages(messagesPerConsumer);
        assertTrue(consumer.getCount() == messagesPerConsumer);
    }

    @Test
    public void testProducerConsumer() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(32);
        QueueRouter queueRouter = new QueueRouter("test");

        TestProducer producer = new TestProducer(queueRouter);

        TestConsumer consumer = new TestConsumer(queueRouter);

        executor.submit(producer);
        executor.submit(consumer);

        producer.submitTask(TaskAction.ATTACH);

        producer.submitTask(TaskAction.SEND_MESSAGE, 1000);
        consumer.submitTask(TaskAction.CHECK_RECEIVED_MESSAGE_COUNT, 0);
        consumer.submitTask(TaskAction.ATTACH);
        consumer.submitTask(TaskAction.CHECK_RECEIVED_MESSAGE_COUNT, 2000);
        producer.submitTask(TaskAction.SEND_MESSAGE, 1000);
        consumer.submitTask(TaskAction.ACK_MESSAGE, 1000);
        producer.submitTask(TaskAction.CHECK_RECEIVED_ACK_COUNT, 2000);
        consumer.submitTask(TaskAction.ACK_MESSAGE, 1000);

        producer.submitTask(TaskAction.DETACH);
        consumer.submitTask(TaskAction.DETACH);

        producer.submitTask(TaskAction.SHUTDOWN);
        consumer.submitTask(TaskAction.SHUTDOWN);

        producer.waitUntilDone();
        consumer.waitUntilDone();

        assertTrue(queueRouter.isCompletelyDetached());

        executor.shutdown();
    }

    @Test
    public void testProducerMultipleConsumers() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(32);
        QueueRouter queueRouter = new QueueRouter("test");

        TestProducer producer = new TestProducer(queueRouter);
        TestConsumer consumer = new TestConsumer(queueRouter);
        TestConsumer consumer2 = new TestConsumer(queueRouter);

        executor.submit(producer);
        executor.submit(consumer);
        executor.submit(consumer2);

        producer.submitTask(TaskAction.ATTACH);
        consumer.submitTask(TaskAction.ATTACH);
        consumer2.submitTask(TaskAction.ATTACH);

        Thread.sleep(5000);

        consumer2.submitTask(TaskAction.CHECK_RECEIVED_MESSAGE_COUNT, 1000);
        consumer.submitTask(TaskAction.CHECK_RECEIVED_MESSAGE_COUNT, 1000);

        producer.submitTask(TaskAction.SEND_MESSAGE, 2000);
        consumer.submitTask(TaskAction.ACK_MESSAGE, 1000);
        consumer2.submitTask(TaskAction.ACK_MESSAGE, 1000);

        producer.waitForAcks(2000);

        producer.submitTask(TaskAction.DETACH);
        consumer.submitTask(TaskAction.DETACH);
        consumer2.submitTask(TaskAction.DETACH);

        producer.submitTask(TaskAction.SHUTDOWN);
        consumer.submitTask(TaskAction.SHUTDOWN);
        consumer2.submitTask(TaskAction.SHUTDOWN);

        producer.waitUntilDone();
        consumer.waitUntilDone();
        consumer2.waitUntilDone();

        assertTrue(queueRouter.isCompletelyDetached());

        executor.shutdown();
    }

    static void sendMessages(int messageCount, QueueRouter queueRouter, MockProducerSink producer) {
        for (int i = 0; i < messageCount; i++) {
            DoveMQMessage message = MessageFactory.createMessage();
            DoveMQMessageImpl messageImpl = ((DoveMQMessageImpl) message);
            messageImpl.setDeliveryId(deliveryIds.getAndIncrement());
            queueRouter.messageReceived(message, producer);
        }
    }
}
