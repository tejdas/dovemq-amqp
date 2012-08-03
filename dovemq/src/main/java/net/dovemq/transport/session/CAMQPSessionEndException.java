package net.dovemq.transport.session;

class CAMQPSessionEndException extends RuntimeException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    CAMQPSessionEndException(String message) {
        super(message);
    }

    CAMQPSessionEndException(String message, Throwable cause) {
        super(message, cause);
    }
}
