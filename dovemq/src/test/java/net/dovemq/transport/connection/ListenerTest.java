package net.dovemq.transport.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import junit.framework.TestCase;

public class ListenerTest extends TestCase
{
    public ListenerTest(String name)
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
    
    public void testCAMQPListener()
    {
        CAMQPConnectionManager.initialize("broker");
        CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();
        try
        {
            CAMQPListener listener = CAMQPListener.createCAMQPListener(defaultConnectionProps);
            listener.start();            
            Socket socket = new Socket(InetAddress.getLocalHost(), CAMQPConnectionConstants.AMQP_IANA_PORT);
            assertTrue(true);
            socket.close();
            Thread.sleep(2000);
            listener.shutdown();
        } catch (UnknownHostException e)
        {
            assertTrue(false);
        } catch (IOException e)
        {
            assertTrue(false);
        } catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void testCAMQPBadHandshake()
    {
        CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();
        try
        {
            CAMQPListener listener = CAMQPListener.createCAMQPListener(defaultConnectionProps);
            listener.start();            
            Socket socket = new Socket(InetAddress.getLocalHost(), CAMQPConnectionConstants.AMQP_IANA_PORT);
            assertTrue(true);
            byte[] badHeader = new byte[] {'H', 'T', 'T', 'P', 1, 0, 0, 0};
            socket.getOutputStream().write(badHeader);
            InputStream in = socket.getInputStream();
            byte[] response = new byte[8];
            in.read(response);
            assertTrue(CAMQPHeaderUtil.validateAMQPHeader(response));
            assertTrue(-1 == in.read());            
            socket.close();
            Thread.sleep(2000);
            listener.shutdown();
        } catch (UnknownHostException e)
        {
            assertTrue(false);
        } catch (IOException e)
        {
            assertTrue(false);
        } catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }    
}

