package net.dovemq.samples.ack;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQEndpointPolicy;
import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ consumer that creates
 * a transient queue in the DoveMQ broker, and waits for incoming
 * messages. The consumer is created with a CONSUMER_ACKS mode,
 * which means the receiver needs to explicitly acknowledge
 * receipt of the message.
 */
public class BasicAckConsumer
{
    private static volatile boolean doShutdown = false;

    /**
     * Implementation of a sample MessageReceiver callback,
     * that is registered with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver
    {
        public SampleMessageReceiver(Consumer consumer)
        {
            super();
            this.consumer = consumer;
        }

        @Override
        public void messageReceived(DoveMQMessage message)
        {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message: " + payload);

            /*
             * Explicitly acknowledge the message receipt.
             */
            consumer.acknowledge(message);
        }

        private final Consumer consumer;
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
         * Read the broker IP address passed in as -Ddovemq.broker
         * Defaults to localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("ackConsumer");

        /*
         * Create an AMQP session.
         */
        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        /*
         * Create a consumer that binds to a transient queue on the broker.
         * Also set the MessageAcknowledgementPolicy to CONSUMER_ACKS.
         */
        DoveMQEndpointPolicy endpointPolicy = new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS);
        Consumer consumer = session.createConsumer("firstQueue", endpointPolicy);

        /*
         * Register a message receiver with the consumer to asynchronously
         * receive messages.
         */
        SampleMessageReceiver messageReceiver = new SampleMessageReceiver(consumer);
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

        /*
         * Shutdown DoveMQ runtime.
         */
        ConnectionFactory.shutdown();
    }
}
