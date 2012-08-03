package net.dovemq.transport.session;

class CAMQPSessionControlWrapper
{
    CAMQPSessionControlWrapper(int channelNumber, Object sessionControl)
    {
        super();
        this.channelNumber = channelNumber;
        this.sessionControl = sessionControl;
    }
    
    CAMQPSessionControlWrapper(Object sessionControl)
    {
        super();
        this.sessionControl = sessionControl;
    }
    
    int getChannelNumber()
    {
        return channelNumber;
    }
    void setChannelNumber(int channelNumber)
    {
        this.channelNumber = channelNumber;
    }
    Object getSessionControl()
    {
        return sessionControl;
    }
    boolean isControlInitiator()
    {
        return isControlInitiator;
    }
    void setControlInitiator(boolean isControlInitiator)
    {
        this.isControlInitiator = isControlInitiator;
    }
    private boolean isControlInitiator = false;
    private int channelNumber = 0;    
    private final Object sessionControl;
}
