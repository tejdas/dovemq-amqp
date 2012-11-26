package net.dovemq.samples.tag_routing;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Publisher;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ publisher that
 * creates/binds to a Topic on the DoveMQ broker, and publishes messages.
 *
 * It also demonstrates how the message is sent with a routing tag.
 */
public class TopicPublisher
{
    public static void main(String[] args)
    {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker
         * Defaults to localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("publisher");

        /*
         * Create an AMQP session.
         */
        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        /*
         * Create a publisher that creates/binds to a topic on the broker.
         */
        Publisher publisher = session.createPublisher("sampleTopic");

        /*
         * Create and publish a message, with a matching routing tag, so subscriber should see it.
         */
        DoveMQMessage message = MessageFactory.createMessage();
        String msg = "Hello from Publisher, the subscriber should get this message";
        System.out.println("publishing message: " + msg);
        message.addPayload(msg.getBytes());
        message.setRoutingTag("abcdefyz");
        publisher.publishMessage(message);

        /*
         * Publish another message with no routing tag, so it should not be sent to subscriber.
         */
        String secondmsg = "Hello again from Publisher, second message: the subscriber should not get this message";
        System.out.println("sending another message: " + secondmsg);
        publisher.publishMessage(secondmsg.getBytes());

        /*
         * Create and publish a message with a routing tag that does not match
         */
        DoveMQMessage message3 = MessageFactory.createMessage();
        String msg3 = "Hello from Publisher, third message: the subscriber should not get this message";
        System.out.println("publishing message: " + msg3);
        message3.addPayload(msg3.getBytes());
        message3.setRoutingTag("abcmnopdefyz");
        publisher.publishMessage(message3);

        /*
         * Close the AMQP session
         */
        session.close();

        /*
         * Shutdown DoveMQ runtime.
         */
        ConnectionFactory.shutdown();
    }
}
