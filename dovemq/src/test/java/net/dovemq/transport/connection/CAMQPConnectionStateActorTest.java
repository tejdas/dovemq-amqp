package net.dovemq.transport.connection;

import org.jboss.netty.channel.Channel;

import net.dovemq.transport.connection.mockjetty.MockJettyChannel;

import junit.framework.TestCase;

public class CAMQPConnectionStateActorTest extends TestCase
{
    public CAMQPConnectionStateActorTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void
    testCreateStateActor()
    {
        CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();        
        CAMQPConnectionStateActor stateActor =
            new CAMQPConnectionStateActor(true, defaultConnectionProps);
        
        Channel channel = new MockJettyChannel(true);
        stateActor.setChannel(channel);
        CAMQPSender sender = stateActor.sender;
        assertTrue(sender.getChannel() == channel);        
        sender.close();
        sender.waitForClose();
        assertTrue(sender.isClosed());        
        //stateActor.initiateHandshake(connectionProps)
    }
}

