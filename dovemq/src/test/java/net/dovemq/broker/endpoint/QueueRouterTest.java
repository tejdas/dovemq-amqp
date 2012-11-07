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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPMessage;

import org.junit.Test;

public class QueueRouterTest  extends TestCase
{
    private static class MockConsumerProxy implements CAMQPSourceInterface
    {
        private final AtomicInteger messageCount = new AtomicInteger(0);
        public int getCount()
        {
            return messageCount.get();
        }

        @Override
        public void registerDispositionObserver(CAMQPMessageDispositionObserver observer)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void sendMessage(DoveMQMessage message)
        {
            messageCount.incrementAndGet();
        }

        @Override
        public CAMQPMessage getMessage()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getMessageCount()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void messageSent(long deliveryId, CAMQPMessage message)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private static class MockProducerSink implements CAMQPTargetInterface
    {
        @Override
        public void registerMessageReceiver(CAMQPMessageReceiver messageReceiver)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean isMessageSettledByPeer, Object newState)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void acknowledgeMessageProcessingComplete(long deliveryId)
        {
            // TODO Auto-generated method stub

        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testBasic()
    {
        QueueRouter queueRouter = new QueueRouter("test");

        MockConsumerProxy consumer1 = new MockConsumerProxy();
        MockConsumerProxy consumer2 = new MockConsumerProxy();
        MockConsumerProxy consumer3 = new MockConsumerProxy();

        queueRouter.consumerAttached(consumer1);
        queueRouter.consumerAttached(consumer2);
        queueRouter.consumerAttached(consumer3);

        MockProducerSink producer = new MockProducerSink();
        queueRouter.producerAttached(producer);

        int messagesPerConsumer = 100;
        for (int i = 0; i < 3*messagesPerConsumer; i++)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            queueRouter.messageReceived(message, producer);
        }

        assertTrue(consumer1.getCount() == messagesPerConsumer);
        assertTrue(consumer2.getCount() == messagesPerConsumer);
        assertTrue(consumer3.getCount() == messagesPerConsumer);
    }
}
