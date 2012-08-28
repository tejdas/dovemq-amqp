package net.dovemq.transport.connection;

import java.io.IOException;

public class ConnectionSysTestServer
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        CAMQPConnectionManager.initialize("broker");
        CAMQPConnectionManager.registerConnectionObserver(new ConnectionObserver());
        CAMQPConnectionProperties defaultConnectionProps =
            CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener =
                CAMQPListener.createCAMQPListener(defaultConnectionProps);

        listener.start();
        System.out.println("AMQP broker: container ID: " + CAMQPConnectionManager.getContainerId());
        ConnectionSysTestCmdDriver.processConsoleInput(true);
        listener.shutdown();
    }
}

