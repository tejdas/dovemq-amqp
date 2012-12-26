package net.dovemq.samples.hierarchy_routing;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;
import net.dovemq.api.Subscriber;

/**
 * This sample shows how to create a DoveMQ Hierarchical subscriber that creates
 * or binds to a certain hierarchy under the root topic and waits for incoming
 * messages. It only gets messages that are published at or above that hierarchy.
 */
public class TopicSubscriber
{
    private static final String ROOT_TOPIC_NAME = "HierarchyRoutingTopic";
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
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("subscriber");

        try
        {
            /*
             * Create an AMQP session.
             */
            final Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a subscriber that creates/binds to a topic on the broker.
             * Also specify the hierarchical scope, so only those messages that
             * are published at/above this hierarchy are routed to the subscriber.
             */
            String hierarchicalTopicName = ROOT_TOPIC_NAME + ".foo.bar";
            Subscriber subscriber = session.createHierarchicalTopicSubscriber(hierarchicalTopicName);

            /*
             * Register a message receiver with the consumer to asynchronously
             * receive messages.
             */
            SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
            subscriber.registerMessageReceiver(messageReceiver);

            /*
             * Register a shutdown hook to perform graceful shutdown.
             */
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run()
                {
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
        }
        catch (Exception ex)
        {
            System.out.println("Caught Exception: " + ex.toString());
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
