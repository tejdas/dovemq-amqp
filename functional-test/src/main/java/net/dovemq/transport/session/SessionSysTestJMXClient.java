package net.dovemq.transport.session;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.ConnectionCommand;

public class SessionSysTestJMXClient
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
        String performIO = args[5];
        
        System.out.println("linkReceiverFactory: " + linkReceiverFactory);
        String brokerContainerId = String.format("broker@%s", brokerIp);
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);

        CAMQPConnectionManager.initialize(publisherName);
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        
        ConnectionCommand localConnectionCommand = new ConnectionCommand();
        SessionCommand localSessionCommand = new SessionCommand();
        localSessionCommand.registerFactory("SysTestCommandReceiver");
        localConnectionCommand.create(brokerContainerId);
        
        Thread.sleep(2000);
        
        SessionCommandMBean mbeanProxy = jmxWrapper.getSessionBean();
        
        mbeanProxy.registerFactory(linkReceiverFactory);
          
        SessionIOTestUtils.createSessions(numThreads, brokerContainerId, localSessionCommand);
        
        /*
         * Check and assert the number of sessions created on the CAMQP Broker
         */
        Collection<Integer> attachedChannels = mbeanProxy.getChannelId(CAMQPConnectionManager.getContainerId());
        assertTrue(attachedChannels.size() == numThreads);
        
        if (performIO.equalsIgnoreCase("true"))
        {
            SessionIOTestUtils.sendTransferFrames(numThreads, brokerContainerId, localSessionCommand);
            System.out.println("waiting for IO to be done");
            while (true)
            {
                Thread.sleep(5000);
                if (mbeanProxy.isIODone())
                    break;
            }
        }
        
        SessionIOTestUtils.closeSessions(numThreads, brokerContainerId, localSessionCommand);
        
        localConnectionCommand.close(brokerContainerId);        
        assertTrue(localConnectionCommand.checkClosed(brokerContainerId));
        
        SessionIOTestUtils.cleanup();
        
        jmxWrapper.cleanup();
    }
}
