package net.dovemq.transport.connection;

class CAMQPListenerShutdownHook extends Thread
{
    private final CAMQPListener managedClass;

    CAMQPListenerShutdownHook(CAMQPListener managedClass)
    {
        super();
        this.managedClass = managedClass;
    }

    @Override
    public void run()
    {
        try
        {
            managedClass.shutdown();
        } catch (Exception ee)
        {
            ee.printStackTrace();
        }
    }
}
