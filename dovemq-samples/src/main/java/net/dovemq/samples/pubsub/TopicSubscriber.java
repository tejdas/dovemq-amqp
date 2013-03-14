package net.dovemq.samples.pubsub;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;
import net.dovemq.api.Subscriber;

/**
 * This sample shows how to create a DoveMQ subscriber that creates or binds to
 * a topic on the DoveMQ broker, and waits for incoming messages.
 */
public class TopicSubscriber {
    private static final String TOPIC_NAME = "SampleTopic";

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Subscriber.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        @Override
        public void messageReceived(DoveMQMessage message) {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message: " + payload);
        }
    }

    public static void main(String[] args) {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("subscriber");

        try {
            /*
             * Create an AMQP session.
             */
            final Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a subscriber that creates/binds to a topic on the broker.
             */
            Subscriber subscriber = session.createSubscriber(TOPIC_NAME);

            /*
             * Register a message receiver with the subscriber to asynchronously
             * receive messages.
             */
            SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
            subscriber.registerMessageReceiver(messageReceiver);

            System.out.println("waiting for messages. Press Ctl-C to shut down subscriber.");
            /*
             * Register a shutdown hook to perform graceful shutdown.
             */
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    /*
                     * Close the AMQP session
                     */
                    session.close();

                    /*
                     * Shutdown DoveMQ runtime.
                     */
                    ConnectionFactory.shutdown();
                }
            });
        }
        catch (Exception ex) {
            System.out.println("Caught Exception: " + ex.toString());
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
