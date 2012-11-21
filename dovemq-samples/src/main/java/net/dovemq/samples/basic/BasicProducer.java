package net.dovemq.samples.basic;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

public class BasicProducer
{
    public static void main(String[] args)
    {
        String brokerIp = System.getProperty("dovemq.broker", "localhost");
        ConnectionFactory.initialize("producer");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        Producer producer = session.createProducer("firstQueue");

        DoveMQMessage message = MessageFactory.createMessage();
        String msg = "Hello from Producer";
        System.out.println("sending message: " + msg);
        message.addPayload(msg.getBytes());
        producer.sendMessage(message);

        String secondmsg = "Hello again from Producer, second message";
        System.out.println("sending another message: " + secondmsg);
        producer.sendMessage(secondmsg.getBytes());

        session.close();
        ConnectionFactory.shutdown();
    }
}
