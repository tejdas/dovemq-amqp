package demo.basic.consumer;

import java.util.Collection;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.Session;

public class BasicConsumer
{
    private static volatile boolean doShutdown = false;
    private static class TestMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            Collection<byte[]> body = message.getPayloads();
            for (byte[] b : body)
            {
                String bString = new String(b);
                System.out.println(bString);
            }
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

        ConnectionFactory.initialize("consumer");

        Session session = ConnectionFactory.createSession("localhost");

        Consumer consumer = session.createConsumer("firstQueue");

        TestMessageReceiver messageReceiver = new TestMessageReceiver();
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

        ConnectionFactory.shutdown();
    }
}
