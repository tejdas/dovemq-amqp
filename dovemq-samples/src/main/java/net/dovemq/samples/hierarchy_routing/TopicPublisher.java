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

            {
                /*
                 * published above subscriber's hierarchy, so it should not receive it.
                 */
                String topicHierarchy = "sports";
                Publisher publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
                DoveMQMessage message = createMessage("First message: Published at hierarchy: " + topicHierarchy);
                publisher.publishMessage(message);
            }

            {
                /*
                 * published at subscriber's hierarchy, so it should receive it.
                 */
                String topicHierarchy = "sports.cricket";
                Publisher publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
                DoveMQMessage message = createMessage("Second message: Published at hierarchy: " + topicHierarchy);
                publisher.publishMessage(message);
            }

            {
                /*
                 * published under subscriber's hierarchy, so it should receive it.
                 */
                String topicHierarchy = "sports.cricket.test";
                Publisher publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
                DoveMQMessage message = createMessage("Third message: Published at hierarchy: " + topicHierarchy);
                publisher.publishMessage(message);
            }

            {
                /*
                 * Not published at/under subscriber's hierarchy, so it should not receive it.
                 */
                String topicHierarchy = "sports.cricketers";
                Publisher publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
                DoveMQMessage message = createMessage("Fourth message: Published at hierarchy: " + topicHierarchy);
                publisher.publishMessage(message);
            }

            {
                /*
                 * The following example shows how the Publisher's topic hierarchy scope could be overridden by
                 * a message-specific hierarchy scope.
                 *
                 * Publisher not bound at/under subscriber's hierarchy, but we are setting
                 * a hierarchy scope directly on the message, that is under subscriber's hierarchy,
                 * so it should receive it.
                 */
                String topicHierarchy = "sports";
                Publisher publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
                String messageLevelHierarchy = "sports.cricket.odi";
                DoveMQMessage message = createMessage("Fifth message: Published at hierarchy: " + messageLevelHierarchy);
                message.setTopicPublishHierarchy(messageLevelHierarchy);
                publisher.publishMessage(message);
            }

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

    private static DoveMQMessage createMessage(String messageBody) {
        DoveMQMessage message = MessageFactory.createMessage();
        System.out.println("publishing message: " + messageBody);
        message.addPayload(messageBody.getBytes());
        return message;
    }
}
