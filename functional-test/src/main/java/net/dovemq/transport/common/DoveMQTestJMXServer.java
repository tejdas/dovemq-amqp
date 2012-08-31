package net.dovemq.transport.common;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.dovemq.transport.connection.ConnectionCommand;
import net.dovemq.transport.connection.ConnectionCommandMBean;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.LinkCommand;
import net.dovemq.transport.link.LinkCommandMBean;
import net.dovemq.transport.session.SessionCommand;
import net.dovemq.transport.session.SessionCommandMBean;

public class DoveMQTestJMXServer
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        CAMQPLinkManager.initialize(true, "broker");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ConnectionCommandMBean connectionCommandBean = new ConnectionCommand();
        registerBean(mbs, connectionCommandBean, JMXConstants.CONNECTION_COMMAND_BEAN);
        
        SessionCommandMBean sessionCommandBean = new SessionCommand();
        registerBean(mbs, sessionCommandBean, JMXConstants.SESSION_COMMAND_BEAN);
        
        LinkCommandMBean linkCommandBean = new LinkCommand();
        registerBean(mbs, linkCommandBean, JMXConstants.LINK_COMMAND_BEAN);        
 
        ConnectionCommand beanImpl = (ConnectionCommand) connectionCommandBean;
        while (!beanImpl.isShutdown())
        {
            Thread.sleep(1000);
        }

        CAMQPLinkManager.shutdown();
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
