package net.dovemq.transport.session;

import java.util.List;

public class CAMQPSessionFactory
{
    private static final CAMQPSessionFactory sessionFactory = new CAMQPSessionFactory();

    private CAMQPSessionFactory()
    {
    }

    public static CAMQPSessionInterface getOrCreateCAMQPSession(String targetContainerId)
    {
        List<CAMQPSession> sessionList = CAMQPSessionManager.getAllSessions(targetContainerId);
        if (sessionList.isEmpty())
            return sessionFactory.createSession(targetContainerId);
        
        return sessionList.get(0); // REVISIT TODO get the session with minimal linkReceivers attached       
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Could not open the session to: " + targetContainerId); 
        }
        
        return session;
    }
}
