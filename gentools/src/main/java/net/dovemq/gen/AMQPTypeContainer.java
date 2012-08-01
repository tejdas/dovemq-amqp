package net.dovemq.gen;

interface AMQPTypeContainer
{
    public void
    display();
    
    public boolean
    contains(String typeName);
    
    public AMQPType
    getType(String typeName);
}
