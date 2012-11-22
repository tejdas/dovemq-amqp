package net.dovemq.samples.pubsub;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;
import net.dovemq.api.Subscriber;

public class TopicSubscriber
{
    private static volatile boolean doShutdown = false;
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

        String brokerIp = System.getProperty("dovemq.broker", "localhost");
        ConnectionFactory.initialize("subscriber");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        Subscriber subscriber = session.createSubscriber("sampleTopic");

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

        session.close();
        ConnectionFactory.shutdown();
    }
}
