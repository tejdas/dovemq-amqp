package net.dovemq.broker.driver;

import net.dovemq.transport.link.CAMQPLinkManager;

public class DoveMQBrokerDriver
{
    private static volatile boolean doShutdown = false;

    static void shutdown()
    {
        doShutdown = true;
    }

    public static void main(String[] args)
    {
        final DoveMQBrokerShutdownHook sh = new DoveMQBrokerShutdownHook();
        Runtime.getRuntime().addShutdownHook(sh);

        CAMQPLinkManager.initialize(true, "broker");
        while (!doShutdown)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("DoveMQ broker shutting down");
        CAMQPLinkManager.shutdown();
        System.out.println("DoveMQ broker shut down");
    }
}
