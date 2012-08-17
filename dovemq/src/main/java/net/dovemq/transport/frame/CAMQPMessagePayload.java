package net.dovemq.transport.frame;

import java.util.Arrays;

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
        this.payload = Arrays.copyOf(payload, payload.length);
    }

    private final byte[] payload;    
}
