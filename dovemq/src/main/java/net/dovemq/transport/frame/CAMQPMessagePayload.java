package net.dovemq.transport.frame;

import net.jcip.annotations.Immutable;

@Immutable
public class CAMQPMessagePayload
{
    public byte[] getPayload()
    {
        return payload;
    }
    
    public CAMQPMessagePayload(byte[] payload)
    {
        super();
        this.payload = payload;
    }

    private final byte[] payload;    
}
