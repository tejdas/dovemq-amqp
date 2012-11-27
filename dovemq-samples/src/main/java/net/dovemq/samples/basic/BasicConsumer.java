package net.dovemq.samples.basic;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ consumer that creates a transient
 * queue in the DoveMQ broker, and waits for incoming messages.
 */
public class BasicConsumer
{
    private static volatile boolean doShutdown = false;

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Consumer.
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
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("consumer");

        try
        {
            /*
             * Create an AMQP session.
             */
            Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a consumer that binds to a transient queue on the broker.
             */
            Consumer consumer = session.createConsumer("firstQueue");

            /*
             * Register a message receiver with the consumer to asynchronously
             * receive messages.
             */
            SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
            consumer.registerMessageReceiver(messageReceiver);

            System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
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
