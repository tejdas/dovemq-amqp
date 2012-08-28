package net.dovemq.transport.link;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;
import net.dovemq.transport.connection.ConnectionObserver;
import net.dovemq.transport.session.CAMQPSessionManager;

public class LinkTestBroker
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        CAMQPConnectionManager.initialize("broker");
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        CAMQPConnectionManager.registerConnectionObserver(new ConnectionObserver());
        CAMQPConnectionProperties defaultConnectionProps =
            CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener =
                CAMQPListener.createCAMQPListener(defaultConnectionProps);
        listener.start();

        CAMQPLinkManager.initialize();
        
        System.out.println("Press any key to continue");
      
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        while ((s = in.readLine()) != null && s.length() != 0)
        {
            if (s.equalsIgnoreCase("done"))
                break;
        }
        
        System.out.println("shutting down");
        CAMQPSessionManager.shutdown();
        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        
        listener.shutdown();
    }
}
