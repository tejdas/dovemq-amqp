package net.dovemq.transport.session;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.ConnectionCommand;

public class SessionSysTestBiDir
{
    public static void main(String[] args) throws InterruptedException, IOException, CAMQPSessionBeginException, MalformedObjectNameException
    {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];
        int numThreads = Integer.valueOf(args[3]);
        String linkReceiverFactory = args[4];
        
        String brokerContainerId = String.format("broker@%s", brokerIp);
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);

        CAMQPConnectionManager.initialize(publisherName);
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        
        ConnectionCommand localConnectionCommand = new ConnectionCommand();
        SessionCommand localSessionCommand = new SessionCommand();
        localSessionCommand.registerFactory(linkReceiverFactory);
        localConnectionCommand.create(brokerContainerId);
        
        Thread.sleep(2000);
        
        SessionCommandMBean mbeanProxy = jmxWrapper.getSessionBean();
        
        mbeanProxy.registerFactory(linkReceiverFactory);
          
        SessionIOTestUtils.createSessions(numThreads, brokerContainerId, localSessionCommand);
        
        /*
         * Create sessions on the AMQP connection from the broker side
         */
        mbeanProxy.sessionCreateMT(CAMQPConnectionManager.getContainerId(), numThreads);
        
        /*
         * Check and assert the number of sessions created on the CAMQP Broker
         */
        Collection<Integer> attachedChannels = mbeanProxy.getChannelId(CAMQPConnectionManager.getContainerId());
        assertTrue(attachedChannels.size() == numThreads*2);
        
        /*
         * Check and assert the number of sessions created on the CAMQP Broker
         */
        Collection<Integer> attachedChannelsFromBroker = localSessionCommand.getChannelId(brokerContainerId);
        assertTrue(attachedChannelsFromBroker.size() == numThreads*2);
               
        SessionIOTestUtils.closeSessions(numThreads, brokerContainerId, localSessionCommand);
        
        localConnectionCommand.close(brokerContainerId);        
        assertTrue(localConnectionCommand.checkClosed(brokerContainerId));
        
        SessionIOTestUtils.cleanup();
        
        jmxWrapper.cleanup();
    }
}
