package net.dovemq.transport.frame;

public class CAMQPFrameConstants
{
    public static final int FRAME_HEADER_SIZE = 8;
    public static final int DEFAULT_DATA_OFFSET = 2;    
    public static final int AMQP_FRAME_TYPE = 0x00;
    
    public static final int FRAME_TYPE_CONNECTION = 0;
    public static final int FRAME_TYPE_SESSION = 1;
    public static final int FRAME_TYPE_LINK = 2;      
}
