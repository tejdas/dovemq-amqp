package net.dovemq.samples.rpc;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

public class Requester
{
    private static final ConcurrentMap<String, DoveMQMessage> outstandingRequests = new ConcurrentHashMap<String, DoveMQMessage>();

    private static class SampleMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            String correlationId = message.getMessageProperties().getCorrlelationId();
            System.out.println(correlationId);
            DoveMQMessage requestMessage = outstandingRequests.remove(correlationId);
            if (requestMessage != null)
            {
                System.out.println("received response for requestId: " + correlationId);

                byte[] body = message.getPayload();
                String payload = new String(body);
                System.out.println("Response payload: " + payload);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        String brokerIp = System.getProperty("dovemq.broker", "localhost");
        ConnectionFactory.initialize("producer");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        String toAddress = "requestQueue";
        String replyToAddreess = "responseQueue";

        Producer producer = session.createProducer(toAddress);

        Consumer consumer = session.createConsumer(replyToAddreess);
        consumer.registerMessageReceiver(new SampleMessageReceiver());

        DoveMQMessage message = MessageFactory.createMessage();
        String messageId = UUID.randomUUID().toString();
        message.getMessageProperties().setMessageId(messageId);
        message.getMessageProperties().setReplyToAddress(replyToAddreess);
        outstandingRequests.put(messageId,  message);

        String msg = "Request from Producer";
        System.out.println("sending message: " + msg);
        message.addPayload(msg.getBytes());
        producer.sendMessage(message);

        System.out.println("waiting for response");
        Thread.sleep(10000);

        session.close();
        ConnectionFactory.shutdown();
    }
}
