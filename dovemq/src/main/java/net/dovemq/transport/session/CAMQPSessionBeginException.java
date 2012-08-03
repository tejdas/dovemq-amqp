package net.dovemq.transport.session;

class CAMQPSessionBeginException extends RuntimeException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    CAMQPSessionBeginException(String message) {
        super(message);
    }

    CAMQPSessionBeginException(String message, Throwable cause) {
        super(message, cause);
    }
}
