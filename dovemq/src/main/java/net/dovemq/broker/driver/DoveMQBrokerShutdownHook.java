package net.dovemq.broker.driver;

class DoveMQBrokerShutdownHook extends Thread
{
    DoveMQBrokerShutdownHook()
    {
        super();
    }

    @Override
    public void run()
    {
        DoveMQBrokerDriver.shutdown();
    }
}
