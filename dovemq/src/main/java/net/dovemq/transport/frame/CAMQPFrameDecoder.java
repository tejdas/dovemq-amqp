package net.dovemq.transport.frame;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class CAMQPFrameDecoder extends FrameDecoder
{
    private CAMQPFrameHeader header = null;
    
    private CAMQPFrameHeader getHeaderAndReset()
    {
        CAMQPFrameHeader currentHeader = header;
        header = null;
        return currentHeader;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
            ChannelBuffer buffer) throws Exception
    {
        if (ctx == null || channel == null || buffer == null)
        {
            throw new NullPointerException();
        }
        
        if (header == null)
        {
            if (buffer.readableBytes() < CAMQPFrameConstants.FRAME_HEADER_SIZE)
            {
                return null;
            }
            ChannelBuffer headerBuffer =
                buffer.readBytes(CAMQPFrameConstants.FRAME_HEADER_SIZE);
            header = CAMQPFrameHeaderCodec.decode(headerBuffer);
        }

        int dataOffset = header.getDataOffset();
        int variableHeaderSize = (dataOffset-CAMQPFrameConstants.DEFAULT_DATA_OFFSET) * 4;
        
        int frameBodySize = (int) header.getFrameSize() - (CAMQPFrameConstants.FRAME_HEADER_SIZE + variableHeaderSize);
        if (frameBodySize > 0)
        {
            if (buffer.readableBytes() < frameBodySize)
            {
                return null;
            }
            int readerIndex = buffer.readerIndex();
            ChannelBuffer body = buffer.slice(readerIndex, frameBodySize);
            buffer.readerIndex(readerIndex + frameBodySize);
            return new CAMQPFrame(getHeaderAndReset(), body);
        }
        else
        {
            return new CAMQPFrame(getHeaderAndReset(), null);
        }
    }
}
