package net.dovemq.transport.frame;

import net.jcip.annotations.Immutable;

import org.jboss.netty.buffer.ChannelBuffer;


@Immutable
public class CAMQPHandshakeFrame
{
    public CAMQPHandshakeFrame(ChannelBuffer body)
    {
        super();
        this.body = body;
    }

    public ChannelBuffer getBody()
    {
        return body;
    }
    private final ChannelBuffer body;
}
