package net.dovemq.samples.pubsub;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;
import net.dovemq.api.Subscriber;

/**
 * This sample shows how to create a DoveMQ subscriber that creates
 * or binds to a topic on the DoveMQ broker, and waits for incoming
 * messages.
 */
public class TopicSubscriber
{
    private static volatile boolean doShutdown = false;

    /**
     * Implementation of a sample MessageReceiver callback,
     * that is registered with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message: " + payload);
        }
    }

    public static void main(String[] args)
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                doShutdown = true;
            }
        });

        /*
         * Read the broker IP address passed in as -Dbroker.ip
         * Defaults to localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("subscriber");

        /*
         * Create an AMQP session.
         */
        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        /*
         * Create a subscriber that creates/binds to a topic on the broker.
         */
        Subscriber subscriber = session.createSubscriber("sampleTopic");

        /*
         * Register a message receiver with the consumer to asynchronously
         * receive messages.
         */
        SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
        subscriber.registerMessageReceiver(messageReceiver);

        System.out.println("waiting for messages. Press Ctl-C to shut down subscriber.");
        while (!doShutdown)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

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