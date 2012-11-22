package net.dovemq.samples.ack;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQEndpointPolicy;
import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

public class BasicAckConsumer
{
    private static volatile boolean doShutdown = false;
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

        String brokerIp = System.getProperty("dovemq.broker", "localhost");
        ConnectionFactory.initialize("consumer");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        DoveMQEndpointPolicy endpointPolicy = new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS);
        Consumer consumer = session.createConsumer("firstQueue", endpointPolicy);

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

        session.close();
        ConnectionFactory.shutdown();
    }
}
