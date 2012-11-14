package demo.basic.producer;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

public class BasicProducer
{
    public static void main(String[] args)
    {
        ConnectionFactory.initialize("producer");

        Session session = ConnectionFactory.createSession("localhost");
        System.out.println("created session");

        Producer producer = session.createProducer("firstQueue");

        DoveMQMessage message = MessageFactory.createMessage();
        String msg = "Hello from Producer";
        message.addPayload(msg.getBytes());
        producer.sendMessage(message);
        ConnectionFactory.shutdown();
    }
}
