package net.dovemq.transport.connection;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ConnectionSysTestJMXServer
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        CAMQPConnectionManager.initialize("broker");
        CAMQPConnectionManager.registerConnectionObserver(new ConnectionObserver());
        CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener = CAMQPListener.createCAMQPListener(defaultConnectionProps);

        listener.start();
        System.out.println("AMQP broker: container ID: " + CAMQPConnectionManager.getContainerId());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ConnectionCommandMBean commandBean = new ConnectionCommand();
        ObjectName commandBeanName = null;

        try
        {
            commandBeanName = new ObjectName("CAMQP:name=ConnectionCommandMBean");
            mbs.registerMBean(commandBean, commandBeanName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        ConnectionCommand beanImpl = (ConnectionCommand) commandBean;
        while (!beanImpl.isShutdown())
        {
            Thread.sleep(1000);
        }
        listener.shutdown();
    }
}