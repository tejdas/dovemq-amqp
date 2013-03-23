package net.dovemq.samples.ack;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQEndpointPolicy;
import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ consumer that creates a
 * queue in the DoveMQ broker, and waits for incoming messages. The consumer is
 * created with a CONSUMER_ACKS mode, which means the receiver needs to
 * explicitly acknowledge receipt of the message.
 */
public class BasicAckConsumer {
    private static final String QUEUE_NAME = "SampleQueue";

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        public SampleMessageReceiver(Consumer consumer) {
            super();
            this.consumer = consumer;
        }

        @Override
        public void messageReceived(DoveMQMessage message) {
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

    public static void main(String[] args) {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("ackConsumer");

        try {
            /*
             * Create an AMQP session.
             */
            final Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a consumer that binds to a queue on the broker.
             * Also set the MessageAcknowledgementPolicy to CONSUMER_ACKS.
             */
            DoveMQEndpointPolicy endpointPolicy = new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS);
            Consumer consumer = session.createConsumer(QUEUE_NAME, endpointPolicy);

            /*
             * Register a message receiver with the consumer to asynchronously
             * receive messages.
             */
            SampleMessageReceiver messageReceiver = new SampleMessageReceiver(consumer);
            consumer.registerMessageReceiver(messageReceiver);

            System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
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
