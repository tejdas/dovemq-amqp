package net.dovemq.api;

import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

abstract class BaseMessageReceiver implements CAMQPMessageReceiver {
    private volatile DoveMQMessageReceiver doveMQMessageReceiver = null;
    final CAMQPTargetInterface targetEndpoint;
    private final String targetName;

    public BaseMessageReceiver(String targetName, CAMQPTargetInterface targetEndpoint) {
        super();
        this.targetName = targetName;
        this.targetEndpoint = targetEndpoint;
    }

    public String getTargetName() {
        return targetName;
    }

    /**
     * Register a DoveMQMessageReceiver with the Consumer, so that the receiver
     * will asynchronously receive AMQP messages.
     *
     * @param messageReceiver
     */
    public void registerMessageReceiver(DoveMQMessageReceiver messageReceiver) {
        if (messageReceiver == null) {
            throw new IllegalArgumentException("Null messageReceiver specified");
        }
        doveMQMessageReceiver = messageReceiver;
        targetEndpoint.registerMessageReceiver(this);
    }

    /**
     * Receives an AMQP message from DoveMQ runtime, and dispatches it to the registered
     * DoveMQMessageReceiver.
     */
    @Override
    public void messageReceived(DoveMQMessage message, CAMQPTargetInterface target) {
        doveMQMessageReceiver.messageReceived(message);
    }
}
