package net.dovemq.samples.pubsub;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Publisher;
import net.dovemq.api.Session;

public class TopicPublisher
{
    public static void main(String[] args)
    {
        String brokerIp = System.getProperty("dovemq.broker", "localhost");
        ConnectionFactory.initialize("publisher");

        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        Publisher publisher = session.createPublisher("sampleTopic");

        DoveMQMessage message = MessageFactory.createMessage();
        String msg = "Hello from Publisher";
        System.out.println("publishing message: " + msg);
        message.addPayload(msg.getBytes());
        publisher.sendMessage(message);

        String secondmsg = "Hello again from Publisher, second message";
        System.out.println("sending another message: " + secondmsg);
        publisher.sendMessage(secondmsg.getBytes());

        session.close();
        ConnectionFactory.shutdown();
    }
}
