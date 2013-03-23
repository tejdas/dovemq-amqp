package net.dovemq.samples.round_robin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

/**
 * This sample shows how to create a DoveMQ consumer that creates a
 * queue in the DoveMQ broker, and waits for incoming messages. It creates
 * multiple consumers attached to the queue. When Producer sends messages to the
 * queue, it is routed to all the consumers in a round-robin fashion.
 */
public class RRConsumer {
    private static final String QUEUE_NAME = "SampleQueue";

    private static final int NUM_CONSUMERS = 4;

    private static final CountDownLatch doneSignal = new CountDownLatch(1);

    private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_CONSUMERS);

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        public SampleMessageReceiver(int id) {
            super();
            this.id = id;
        }

        @Override
        public void messageReceived(DoveMQMessage message) {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message by consumer: " + id + " payload: " + payload);
        }

        private final int id;
    }

    private static class SampleConsumer implements Runnable {
        public SampleConsumer(Session session, int id) {
            super();
            this.session = session;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                /*
                 * Create a consumer that binds to a queue on the
                 * broker.
                 */
                Consumer consumer = session.createConsumer(QUEUE_NAME);

                /*
                 * Register a message receiver with the consumer to
                 * asynchronously receive messages.
                 */
                SampleMessageReceiver messageReceiver = new SampleMessageReceiver(id);
                consumer.registerMessageReceiver(messageReceiver);

                System.out.println("created consumer: " + id + " , waiting for messages");
                doneSignal.await();
                System.out.println("consumer: " + id + " done");
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private final Session session;

        private int id;
    }

    public static void main(String[] args) throws InterruptedException {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("consumer");

        try {
            /*
             * Create an AMQP session.
             */
            final Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            for (int i = 0; i < NUM_CONSUMERS; i++) {
                SampleConsumer sampleConsumer = new SampleConsumer(session, i);
                executor.submit(sampleConsumer);
            }

            System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
            /*
             * Register a shutdown hook to perform graceful shutdown.
             */
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    doneSignal.countDown();
                    try {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    executor.shutdown();
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
