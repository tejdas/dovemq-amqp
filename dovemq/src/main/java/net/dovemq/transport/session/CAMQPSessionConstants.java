package net.dovemq.transport.session;

public class CAMQPSessionConstants
{
    protected static final long DEFAULT_OUTGOING_WINDOW_SIZE = 256;
    protected static final long DEFAULT_INCOMING_WINDOW_SIZE = 256;
    protected static final long MIN_INCOMING_WINDOW_SIZE_THRESHOLD = 8;
    protected static final long BATCHED_DISPOSITION_SEND_INTERVAL = 2000L; //milliseconds
}
