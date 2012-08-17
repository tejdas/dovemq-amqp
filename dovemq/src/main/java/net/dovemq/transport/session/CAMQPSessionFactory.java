package net.dovemq.transport.session;

import java.util.List;
import org.apache.log4j.Logger;

/**
 * Factory to create AMQP sessions
 * @author tejdas
 *
 */
public final class CAMQPSessionFactory
{
    private static final Logger log = Logger.getLogger(CAMQPSessionFactory.class);
    private static final CAMQPSessionFactory sessionFactory = new CAMQPSessionFactory();

    private CAMQPSessionFactory()
    {
    }

    /**
     * If a session already exists to the AMQP target, return it. Otherwise, create
     * a new session.
     * 
     * @param targetContainerId
     * @return
     */
    public static CAMQPSessionInterface getOrCreateCAMQPSession(String targetContainerId)
    {
        List<CAMQPSession> sessionList = CAMQPSessionManager.getAllSessions(targetContainerId);
        if (sessionList.isEmpty())
            return sessionFactory.createSession(targetContainerId);
        
        return sessionList.get(0); // TODO get the session with minimal linkReceivers attached       
    }
    
    static CAMQPSession createCAMQPSession()
    {
        return sessionFactory.createSession();
    }

    private CAMQPSession createSession()
    {
        return new CAMQPSession();
    }
    
    private CAMQPSession createSession(String targetContainerId)
    {
        CAMQPSession session = new CAMQPSession();
        
        try
        {
            session.open(targetContainerId);
        } catch (CAMQPSessionBeginException e)
        {
            // TODO
            log.error("Could not open the session to: " + targetContainerId); 
        }
        
        return session;
    }
}
