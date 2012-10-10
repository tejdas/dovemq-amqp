package net.dovemq.api;

import net.dovemq.transport.session.CAMQPSessionInterface;

public class Session
{
    private final CAMQPSessionInterface session;

    public Session(CAMQPSessionInterface session)
    {
        super();
        this.session = session;
    }
}
