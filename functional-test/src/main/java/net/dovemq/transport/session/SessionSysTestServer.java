package net.dovemq.transport.session;

import java.io.IOException;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;

public class SessionSysTestServer
{
    public static void main(String[] args) throws InterruptedException, IOException, CAMQPSessionBeginException
    {
        CAMQPConnectionManager.initialize("broker");
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        CAMQPConnectionProperties defaultConnectionProps =
            CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener =
                CAMQPListener.createCAMQPListener(defaultConnectionProps);
        listener.start();

        SessionSysTestCmdDriver.processConsoleInput(true);

        CAMQPSessionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        listener.shutdown();
    }
}
