package net.dovemq.samples.basic;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ producer that creates a transient
 * queue in the DoveMQ broker, and sends messages.
 */
public class BasicProducer
{
    public static void main(String[] args)
    {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("producer");

        try
        {
            /*
             * Create an AMQP session.
             */
            Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a producer that binds to a transient queue on the broker.
             */
            Producer producer = session.createProducer("firstQueue");

            /*
             * Create and send a message.
             */
            DoveMQMessage message = MessageFactory.createMessage();
            String msg = "Hello from Producer";
            System.out.println("sending message: " + msg);
            message.addPayload(msg.getBytes());
            producer.sendMessage(message);

            /*
             * Send another message.
             */
            String secondmsg = "Hello again from Producer, second message";
            System.out.println("sending another message: " + secondmsg);
            producer.sendMessage(secondmsg.getBytes());

            /*
             * Close the AMQP session
             */
            session.close();
        }
        finally
        {
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
