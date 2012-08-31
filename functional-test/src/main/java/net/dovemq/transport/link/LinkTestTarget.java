package net.dovemq.transport.link;

import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestTarget implements CAMQPTargetInterface
{
    @Override
    public void messageReceived(String deliveryTag, CAMQPMessagePayload message)
    {
        System.out.println("messageReceived: " + deliveryTag);
    }

    @Override
    public void messageStateChanged(String deliveryId,
            int oldState,
            int newState)
    {
        // TODO Auto-generated method stub
        
    }
}
