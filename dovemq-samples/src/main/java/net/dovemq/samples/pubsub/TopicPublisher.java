package net.dovemq.samples.pubsub;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Publisher;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ publisher that
 * creates/binds to a Topic on the DoveMQ broker, and sends messages.
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
         * Create and publish a message.
         */
        DoveMQMessage message = MessageFactory.createMessage();
        String msg = "Hello from Publisher";
        System.out.println("publishing message: " + msg);
        message.addPayload(msg.getBytes());
        publisher.publishMessage(message);

        /*
         * Publish another message.
         */
        String secondmsg = "Hello again from Publisher, second message";
        System.out.println("sending another message: " + secondmsg);
        publisher.publishMessage(secondmsg.getBytes());

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
