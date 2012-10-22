package net.dovemq.api;

import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.session.CAMQPSessionInterface;

public class Session
{
    private final String brokerContainerId;
    private final CAMQPSessionInterface session;

    Session(String brokerContainerId, CAMQPSessionInterface session)
    {
        super();
        this.brokerContainerId = brokerContainerId;
        this.session = session;
    }

    public void close()
    {
        session.close();
    }

    public Producer createProducer(String queueName)
    {
        String source = "src";
        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId, source, queueName, new CAMQPEndpointPolicy());
        return null;
    }

    public Consumer createConsumer(String queueName)
    {
        return null;
    }

    public Publisher createPublisher(String topicName)
    {
        return null;
    }

    public Subscriber createSubscriber(String topicName)
    {
        return null;
    }
}
