package net.dovemq.transport.protocol;

public class CAMQPProtocolConstants
{
    public static final int OCTET = 8;
    protected static final int UBYTE_MAX_VALUE = 255;
    protected static final int USHORT_MAX_VALUE = 65535;
    protected static final long UINT_MAX_VALUE = 4294967295L;
    public static final int INT_MAX_VALUE =   0x7FFFFFFF; // 2147483647;
    
    protected static final int DYNAMIC_BUFFER_INITIAL_SIZE = 4096;
    
    protected static final int DEFAULT_MAX_CHANNELS = 16;
    protected static final int DEFAULT_HEARTBEAT_INTERVAL = 1024;
    
    public static final String CHARSET_UTF8 = "UTF-8";
    protected static final String CHARSET_UTF16 = "UTF-16";    
}
