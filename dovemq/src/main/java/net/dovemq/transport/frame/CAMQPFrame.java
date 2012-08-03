package net.dovemq.transport.frame;

import org.jboss.netty.buffer.ChannelBuffer;

public class CAMQPFrame
{
    public CAMQPFrame(CAMQPFrameHeader header, ChannelBuffer body)
    {
        super();
        this.header = header;
        this.body = body;
    }
    
    public CAMQPFrameHeader getHeader()
    {
        return header;
    }
    public ChannelBuffer getBody()
    {
        return body;
    }
    private final CAMQPFrameHeader header;
    private final ChannelBuffer body;
}

