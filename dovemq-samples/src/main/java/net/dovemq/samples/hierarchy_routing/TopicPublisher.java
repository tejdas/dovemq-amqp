package net.dovemq.samples.hierarchy_routing;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Publisher;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ publisher that creates/binds to a
 * Topic on the DoveMQ broker, and publishes messages. It also demonstrates how
 * the message is targeted to a publish hierarchy scope.
 */
public class TopicPublisher {
    private static final String ROOT_TOPIC_NAME = "HierarchyRoutingTopic";

    public static void main(String[] args) {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("publisher");

        try {
            /*
             * Create an AMQP session.
             */
            Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a publisher that creates/binds to a topic on the broker.
             */
            Publisher publisher = session.createHierarchicalTopicPublisher(ROOT_TOPIC_NAME);

            /*
             * Create and publish a message, without a topic publisher
             * hierarchy, so the subscriber should not get it.
             */
            DoveMQMessage message = MessageFactory.createMessage();
            String msg = "Hello from Publisher, first message, published at hierarchy: " + ROOT_TOPIC_NAME;
            System.out.println("publishing message: " + msg);
            message.addPayload(msg.getBytes());
            publisher.publishMessage(message);

            /*
             * Create and publish a message, with a topic publisher hierarchy,
             * that is not under the subscriber's scope, hence should not get it.
             */
            DoveMQMessage message1 = MessageFactory.createMessage();
            String topicPublisherHierarchy1 = ROOT_TOPIC_NAME + ".foo";
            String msg1 = "Hello from Publisher, second message, published at hierarchy: " + topicPublisherHierarchy1;
            System.out.println("publishing message: " + msg1);
            message1.addPayload(msg1.getBytes());
            message1.setTopicPublishHierarchy(topicPublisherHierarchy1);
            publisher.publishMessage(message1);

            /*
             * Create and publish a message a topic publisher hierarchy, that is under the subscriber's scope
             * and therefore should get it.
             */
            DoveMQMessage message2 = MessageFactory.createMessage();
            String topicPublisherHierarchy2 = ROOT_TOPIC_NAME + ".foo.bar";
            String msg2 = "Hello from Publisher, third message: published at hierarchy: " + topicPublisherHierarchy2 + " the subscriber should get this message";
            System.out.println("publishing message: " + msg2);
            message2.addPayload(msg2.getBytes());
            message2.setTopicPublishHierarchy(topicPublisherHierarchy2);
            publisher.publishMessage(message2);

            /*
             * Create and publish a message a topic publisher hierarchy, that
             * the subscriber is not scoped under, and therefore should not get
             * it.
             */
            DoveMQMessage message3 = MessageFactory.createMessage();
            String topicPublisherHierarchy3 = ROOT_TOPIC_NAME + ".bar";
            String msg3 = "Hello from Publisher, fourth message: published at hierarchy: " + topicPublisherHierarchy3 + " the subscriber should get this message";
            System.out.println("publishing message: " + msg3);
            message3.addPayload(msg3.getBytes());
            message3.setTopicPublishHierarchy(topicPublisherHierarchy3);
            publisher.publishMessage(message3);

            /*
             * Create and publish a message a topic publisher hierarchy, that
             * the subscriber is not scoped under, and therefore should not get
             * it.
             */
            DoveMQMessage message4 = MessageFactory.createMessage();
            String topicPublisherHierarchy4 = ROOT_TOPIC_NAME + "foo.ba";
            String msg4 = "Hello from Publisher, fourth message: published at hierarchy: " + topicPublisherHierarchy4 + " the subscriber should not get this message";
            System.out.println("publishing message: " + msg4);
            message4.addPayload(msg4.getBytes());
            message4.setTopicPublishHierarchy(topicPublisherHierarchy4);
            publisher.publishMessage(message4);

            /*
             * Create and publish a message a topic publisher hierarchy, that
             * the subscriber is not scoped under, and therefore should  get
             * it.
             */
            DoveMQMessage message5 = MessageFactory.createMessage();
            String topicPublisherHierarchy5 = ROOT_TOPIC_NAME + ".foo.bar.nook";
            String msg5 = "Hello from Publisher, fifth message: published at hierarchy: " + topicPublisherHierarchy5 + " the subscriber should not get this message";
            System.out.println("publishing message: " + msg5);
            message5.addPayload(msg5.getBytes());
            message5.setTopicPublishHierarchy(topicPublisherHierarchy5);
            publisher.publishMessage(message5);

            /*
             * Close the AMQP session
             */
            session.close();
        }
        finally {
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
