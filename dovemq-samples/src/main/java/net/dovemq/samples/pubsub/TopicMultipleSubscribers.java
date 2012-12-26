package net.dovemq.samples.pubsub;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;
import net.dovemq.api.Subscriber;

/**
 * This sample shows how to create a DoveMQ subscriber that creates a Topic in
 * the DoveMQ broker, and waits for incoming messages. It creates multiple
 * subscribers attached to the Topic. When Publisher publishes messages on the
 * topic, it is multicasted to all Subscribers.
 */
public class TopicMultipleSubscribers {
    private static final String TOPIC_NAME = "SampleTopic";

    private static final int NUM_SUBSCRIBERS = 4;

    private static final CountDownLatch doneSignal = new CountDownLatch(1);

    private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_SUBSCRIBERS);

    private static volatile boolean doShutdown = false;

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Subscriber.
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

    private static class SampleSubscriber implements Runnable {
        public SampleSubscriber(Session session, int id) {
            super();
            this.session = session;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                /*
                 * Create a subscriber that binds to a topic on the broker.
                 */
                Subscriber subscriber = session.createSubscriber(TOPIC_NAME);

                /*
                 * Register a message receiver with the consumer to
                 * asynchronously receive messages.
                 */
                SampleMessageReceiver messageReceiver = new SampleMessageReceiver(id);
                subscriber.registerMessageReceiver(messageReceiver);

                System.out.println("created subscriber: " + id + " , waiting for messages");
                doneSignal.await();
                System.out.println("subscriber: " + id + " done");
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

            for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
                SampleSubscriber sampleConsumer = new SampleSubscriber(session, i);
                executor.submit(sampleConsumer);
            }

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
                    doShutdown = true;
                }
            });

            System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
            while (!doShutdown) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
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
