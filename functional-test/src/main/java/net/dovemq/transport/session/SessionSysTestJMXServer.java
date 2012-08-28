package net.dovemq.transport.session;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;
import net.dovemq.transport.connection.ConnectionCommand;
import net.dovemq.transport.connection.ConnectionCommandMBean;
import net.dovemq.transport.connection.ConnectionObserver;

public class SessionSysTestJMXServer
{
    static final String CONNECTION_COMMAND_BEAN = "CAMQP:name=ConnectionCommandMBean";
    static final String SESSION_COMMAND_BEAN = "CAMQP:name=SessionCommandMBean";
    
    public static void main(String[] args) throws InterruptedException, IOException, CAMQPSessionBeginException
    {
        CAMQPConnectionManager.initialize("broker");
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        CAMQPConnectionManager.registerConnectionObserver(new ConnectionObserver());
        CAMQPConnectionProperties defaultConnectionProps =
            CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener =
                CAMQPListener.createCAMQPListener(defaultConnectionProps);
        listener.start();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ConnectionCommandMBean connectionCommandBean = new ConnectionCommand();
        registerBean(mbs, connectionCommandBean, CONNECTION_COMMAND_BEAN);
        
        SessionCommandMBean sessionCommandBean = new SessionCommand();
        registerBean(mbs, sessionCommandBean, SESSION_COMMAND_BEAN);
 
        ConnectionCommand beanImpl = (ConnectionCommand) connectionCommandBean;
        while (!beanImpl.isShutdown())
        {
            Thread.sleep(1000);
        }

        CAMQPSessionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        listener.shutdown();
    }
    
    private static void registerBean(MBeanServer mbs, Object commandBean, String beanName)
    {
        try
        {
            ObjectName commandBeanName = new ObjectName(beanName);
            mbs.registerMBean(commandBean, commandBeanName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}