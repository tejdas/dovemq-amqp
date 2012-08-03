package net.dovemq.transport.session;

class CAMQPSessionClosedException extends RuntimeException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    CAMQPSessionClosedException(String message) {
        super(message);
    }

    CAMQPSessionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}