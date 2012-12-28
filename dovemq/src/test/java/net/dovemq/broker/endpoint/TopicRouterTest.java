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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPMessageDispositionObserver;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPMessage;

import org.junit.Test;

public class TopicRouterTest extends TestCase {
    private static final AtomicLong linkIds = new AtomicLong(0L);

    private static DoveMQEndpointManagerImpl endpointManager = null;

    private static class MockSubscriberProxy implements CAMQPSourceInterface {
        private final long id = linkIds.getAndIncrement();

        private final AtomicInteger messageCount = new AtomicInteger(0);

        private volatile String lastReceivedMessageId = null;

        String getLastReceivedMessageId() {
            String msgId = lastReceivedMessageId;
            lastReceivedMessageId = null;
            return msgId;
        }

        @Override
        public void registerDispositionObserver(CAMQPMessageDispositionObserver observer) {
            // TODO Auto-generated method stub
        }

        @Override
        public void sendMessage(DoveMQMessage message) {
            messageCount.incrementAndGet();
            lastReceivedMessageId = message.getMessageProperties()
                    .getMessageId();
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

        @Override
        public long getId() {
            // TODO Auto-generated method stub
            return id;
        }

        public int getCount() {
            return messageCount.get();
        }

    }

    private static class MockPublisherSink implements CAMQPTargetInterface {
        private final long id = linkIds.getAndIncrement();

        @Override
        public void registerMessageReceiver(CAMQPMessageReceiver messageReceiver) {
            // TODO Auto-generated method stub

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
            // TODO Auto-generated method stub

        }

        @Override
        public long getId() {
            // TODO Auto-generated method stub
            return id;
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        endpointManager = new DoveMQEndpointManagerImpl();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        endpointManager = null;
    }

    @Test
    public void testTopicPublishersSameNameDifferentType() {
        CAMQPEndpointPolicy endpointPolicy1 = createPolicy(TopicRouterType.Hierarchical);
        endpointManager.publisherAttached("root", new MockPublisherSink(), endpointPolicy1);
        TopicRouter topicRouter1 = endpointManager.getTopicRouter("root", TopicRouterType.Hierarchical);
        assertNotNull(topicRouter1);
        assertTrue(topicRouter1.getRouterType() == TopicRouterType.Hierarchical);
        assertTrue(endpointPolicy1.getSubscriptionTopicHierarchy() == null);

        CAMQPEndpointPolicy endpointPolicy2 = createPolicy(TopicRouterType.MessageTagFilter);
        endpointManager.publisherAttached("root", new MockPublisherSink(), endpointPolicy2);
        TopicRouter topicRouter2 = endpointManager.getTopicRouter("root", TopicRouterType.MessageTagFilter);
        assertNotNull(topicRouter2);
        assertTrue(topicRouter2.getRouterType() == TopicRouterType.MessageTagFilter);
        assertTrue(endpointPolicy2.getSubscriptionTopicHierarchy() == null);

        CAMQPEndpointPolicy endpointPolicy3 = new CAMQPEndpointPolicy();
        endpointManager.publisherAttached("root", new MockPublisherSink(), endpointPolicy3);
        TopicRouter topicRouter3 = endpointManager.getTopicRouter("root", TopicRouterType.Basic);
        assertNotNull(topicRouter3);
        assertTrue(topicRouter3.getRouterType() == TopicRouterType.Basic);
        assertTrue(endpointPolicy3.getSubscriptionTopicHierarchy() == null);

        assertTrue(topicRouter1 != topicRouter2);
        assertTrue(topicRouter2 != topicRouter3);
    }

    @Test
    public void testTopicSubscribersSameNameDifferentType() {
        CAMQPEndpointPolicy endpointPolicy1 = createPolicy(TopicRouterType.Hierarchical);
        endpointManager.subscriberAttached("root2.foo.bar", new MockSubscriberProxy(), endpointPolicy1);
        TopicRouter topicRouter1 = endpointManager.getTopicRouter("root2", TopicRouterType.Hierarchical);
        assertNotNull(topicRouter1);
        assertTrue(topicRouter1.getRouterType() == TopicRouterType.Hierarchical);
        assertTrue(topicRouter1.getTopicName().equalsIgnoreCase("root2"));
        assertTrue(endpointPolicy1.getSubscriptionTopicHierarchy()
                .equalsIgnoreCase("root2.foo.bar"));

        CAMQPEndpointPolicy endpointPolicy2 = createPolicy(TopicRouterType.MessageTagFilter);
        endpointManager.subscriberAttached("root2", new MockSubscriberProxy(), endpointPolicy2);
        TopicRouter topicRouter2 = endpointManager.getTopicRouter("root2", TopicRouterType.MessageTagFilter);
        assertNotNull(topicRouter2);
        assertTrue(topicRouter2.getRouterType() == TopicRouterType.MessageTagFilter);
        assertTrue(endpointPolicy2.getSubscriptionTopicHierarchy() == null);

        CAMQPEndpointPolicy endpointPolicy3 = new CAMQPEndpointPolicy();
        endpointManager.subscriberAttached("root2", new MockSubscriberProxy(), endpointPolicy3);
        TopicRouter topicRouter3 = endpointManager.getTopicRouter("root2", TopicRouterType.Basic);
        assertNotNull(topicRouter3);
        assertTrue(topicRouter3.getRouterType() == TopicRouterType.Basic);
        assertTrue(endpointPolicy3.getSubscriptionTopicHierarchy() == null);

        assertTrue(topicRouter1 != topicRouter2);
        assertTrue(topicRouter2 != topicRouter3);
    }

    @Test
    public void testTopicHierarchySubscription() {
        CAMQPEndpointPolicy endpointPolicy = createPolicy(TopicRouterType.Hierarchical);
        CAMQPTargetInterface publisher = new MockPublisherSink();
        endpointManager.publisherAttached("root", publisher, endpointPolicy);

        TopicRouter topicRouter = endpointManager.getTopicRouter("root", TopicRouterType.Hierarchical);

        CAMQPEndpointPolicy endpointPolicy1 = createPolicy(TopicRouterType.Hierarchical);
        MockSubscriberProxy subscriber1 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("root.foo.bar", subscriber1, endpointPolicy1);

        CAMQPEndpointPolicy endpointPolicy2 = createPolicy(TopicRouterType.Hierarchical);
        MockSubscriberProxy subscriber2 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("root.foo.bar.nook", subscriber2, endpointPolicy2);

        CAMQPEndpointPolicy endpointPolicy3 = createPolicy(TopicRouterType.Hierarchical);
        MockSubscriberProxy subscriber3 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("root", subscriber3, endpointPolicy3);

        CAMQPEndpointPolicy endpointPolicy4 = createPolicy(TopicRouterType.MessageTagFilter);
        MockSubscriberProxy subscriber4 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("root", subscriber4, endpointPolicy4);

        CAMQPEndpointPolicy endpointPolicy5 = createPolicy(TopicRouterType.Basic);
        MockSubscriberProxy subscriber5 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("root", subscriber5, endpointPolicy5);

        /*
         * Message created with topic root hierarchy tag. All subscribers should get the message.
         */
        DoveMQMessage message = createMessageWithHierarchicalTag("root");
        topicRouter.messageReceived(message, publisher);
        assertEquals(message.getMessageProperties().getMessageId(), subscriber1.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber2.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber3.getLastReceivedMessageId());

        /*
         * Message created with no hierarchy tag. All subscribers should get the message.
         */
        message = createMessage();
        topicRouter.messageReceived(message, publisher);
        assertEquals(message.getMessageProperties().getMessageId(), subscriber1.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber2.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber3.getLastReceivedMessageId());

        /*
         * Subscribers with subscription to root.foo.bar and root.foo.bar.nook should get it.
         */
        message = createMessageWithHierarchicalTag("root.foo.bar");
        topicRouter.messageReceived(message, publisher);
        assertEquals(message.getMessageProperties().getMessageId(), subscriber1.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        /*
         * Subscribers with subscription to root.foo.bar and root.foo.bar.nook should get it.
         */
        message = createMessageWithHierarchicalTag("root.foo");
        topicRouter.messageReceived(message, publisher);
        assertEquals(message.getMessageProperties().getMessageId(), subscriber1.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        /*
         * No subscribers have subscribed to topic hierarchy at or under roo.foo.b
         * Should not be routed at all.
         */
        message = createMessageWithHierarchicalTag("root.foo.b");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        message = createMessageWithHierarchicalTag("root.bar");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        message = createMessageWithHierarchicalTag("root.bar.foo");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        message = createMessageWithHierarchicalTag("root.nook.bar.foo");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        message = createMessageWithHierarchicalTag("root.foo.bar.noo");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        /*
         * Subscriber with subscription to root.foo.bar.nook should get it.
         */
        message = createMessageWithHierarchicalTag("root.foo.bar.nook");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(message.getMessageProperties().getMessageId(), subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        message = createMessageWithHierarchicalTag("root.foo.bar.nook.bunny");
        topicRouter.messageReceived(message, publisher);
        assertEquals(null, subscriber1.getLastReceivedMessageId());
        assertEquals(null, subscriber2.getLastReceivedMessageId());
        assertEquals(null, subscriber3.getLastReceivedMessageId());

        assertTrue(subscriber1.getCount() == 4);
        assertTrue(subscriber2.getCount() == 5);
        assertTrue(subscriber3.getCount() == 2);
        assertTrue(subscriber4.getCount() == 0);
        assertTrue(subscriber5.getCount() == 0);
    }

    @Test
    public void testTopicMessageTagFilterSubscription() {
        CAMQPEndpointPolicy endpointPolicy = createPolicy(TopicRouterType.MessageTagFilter);
        CAMQPTargetInterface publisher = new MockPublisherSink();
        endpointManager.publisherAttached("FilterTopic", publisher, endpointPolicy);
        TopicRouter topicRouter = endpointManager.getTopicRouter("FilterTopic", TopicRouterType.MessageTagFilter);

        CAMQPEndpointPolicy endpointPolicy1 = createPolicy(TopicRouterType.MessageTagFilter);
        endpointPolicy1.setMessageFilterPattern("^[a-z0-9_-]{3,15}$");
        MockSubscriberProxy subscriber1 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("FilterTopic", subscriber1, endpointPolicy1);

        CAMQPEndpointPolicy endpointPolicy2 = createPolicy(TopicRouterType.MessageTagFilter);
        endpointPolicy2.setMessageFilterPattern("([01]?[0-9]|2[0-3]):[0-5][0-9]");
        MockSubscriberProxy subscriber2 = new MockSubscriberProxy();
        endpointManager.subscriberAttached("FilterTopic", subscriber2, endpointPolicy2);

        topicRouter.messageReceived(createMessageWithMessageFilterTag("tejdas"), publisher); // matches
        topicRouter.messageReceived(createMessageWithMessageFilterTag("td"), publisher); // doesn't match
        topicRouter.messageReceived(createMessageWithMessageFilterTag("tej-das"), publisher); // matches
        topicRouter.messageReceived(createMessageWithMessageFilterTag("tej@das"), publisher); // doesn't match
        topicRouter.messageReceived(createMessageWithMessageFilterTag("tej-d_a_s"), publisher); // matches
        topicRouter.messageReceived(createMessageWithMessageFilterTag("tej-123456789-das"), publisher); // doesn't match

        topicRouter.messageReceived(createMessageWithMessageFilterTag("10:33"), publisher); // matches
        topicRouter.messageReceived(createMessageWithMessageFilterTag("02:57"), publisher); // matches
        topicRouter.messageReceived(createMessageWithMessageFilterTag("02: 57"), publisher); // doesn't match
        topicRouter.messageReceived(createMessageWithMessageFilterTag("10:65"), publisher); // doesn't match
        topicRouter.messageReceived(createMessageWithMessageFilterTag("25:38"), publisher); // doesn't match

        assertTrue(subscriber1.getCount() == 3);
        assertTrue(subscriber2.getCount() == 2);
    }

    private static DoveMQMessage createMessageWithHierarchicalTag(String topicHierarchy) {
        DoveMQMessage message = createMessage();
        message.setTopicPublishHierarchy(topicHierarchy);
        return message;
    }

    private static DoveMQMessage createMessageWithMessageFilterTag(String filterTag) {
        DoveMQMessage message = createMessage();
        message.setRoutingTag(filterTag);
        return message;
    }

    private static DoveMQMessage createMessage() {
        DoveMQMessage message = MessageFactory.createMessage();
        message.getMessageProperties().setMessageId(UUID.randomUUID()
                .toString());
        return message;
    }

    private static CAMQPEndpointPolicy createPolicy(TopicRouterType topicRouterType) {
        CAMQPEndpointPolicy endpointPolicy = new CAMQPEndpointPolicy();
        endpointPolicy.setTopicRouterType(topicRouterType);
        return endpointPolicy;
    }
}
