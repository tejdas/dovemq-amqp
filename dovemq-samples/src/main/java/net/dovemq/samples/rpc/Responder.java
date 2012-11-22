package net.dovemq.samples.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

public class Responder
{
    private static volatile boolean doShutdown = false;
    private static final Map<String, Producer> messageProducers = new HashMap<String, Producer>();
    private static final BlockingQueue<DoveMQMessage> incomingRequests = new LinkedBlockingQueue<DoveMQMessage>();

    private static final class RequestProcessor implements Runnable
    {
        public RequestProcessor(Session session)
        {
            super();
            this.session = session;
        }

        @Override
        public void run()
        {
            while (!shutdown)
            {
                DoveMQMessage request = null;
                try
                {
                    request = incomingRequests.poll(1000, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }

                if (request != null)
                {
                    byte[] body = request.getPayload();
                    String payload = new String(body);
                    String replyToAddress = request.getMessageProperties().getReplyToAddress();
                    String messageId = request.getMessageProperties().getMessageId();
                    System.out.println("Received request: " + payload);

                    Producer producer = null;
                    synchronized (messageProducers)
                    {
                        producer = messageProducers.get(replyToAddress);
                        if (producer == null)
                        {
                            producer = session.createProducer(replyToAddress);
                            messageProducers.put(replyToAddress, producer);
                        }
                    }

                    System.out.println("sending response to: " + replyToAddress + " for messageId: " + messageId);
                    DoveMQMessage response = MessageFactory.createMessage();
                    response.getMessageProperties().setCorrlelationId(messageId);
                    response.addPayload("Reply from consumer".getBytes());
                    producer.sendMessage(response);
                }
            }
        }

        private final Session session;
        public volatile boolean shutdown = false;
    }

    private static class SampleMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            incomingRequests.add(message);
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
        ConnectionFactory.initialize("consumer");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        String listenToAddress = "requestQueue";
        Consumer consumer = session.createConsumer(listenToAddress);
        consumer.registerMessageReceiver(new SampleMessageReceiver());

        RequestProcessor requestProcessor = new RequestProcessor(session);
        new Thread(requestProcessor).start();

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

        requestProcessor.shutdown = true;
        session.close();
        ConnectionFactory.shutdown();
    }
}
