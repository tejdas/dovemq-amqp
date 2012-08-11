package net.dovemq.transport.frame;

import net.jcip.annotations.Immutable;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * In-memory representation of AMQP frame
 * @author tejdas
 *
 */
@Immutable
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

