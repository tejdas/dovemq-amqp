package net.dovemq.transport.link;

public class CAMQPLinkConstants
{
    static final boolean ROLE_SENDER  = false;
    static final boolean ROLE_RECEIVER = true;
    
    static final long DEFAULT_MAX_MESSAGE_SIZE = 32768;
    static final long DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER = 1024*1024;
    static final long LINK_CREDIT_VIOLATION_LIMIT = 10L;
}
