package net.dovemq.transport.connection;

import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;
import net.dovemq.transport.frame.CAMQPHandshakeFrame;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Decodes AMQP frames from the byte-stream
 * @author tejdas
 *
 */
class CAMQPFrameDecoder extends FrameDecoder
{
    /*
     * If only the header has been available so far, and not the body,
     * it is stored here, and is used when the remainder of the body
     * is available
     */
    private CAMQPFrameHeader header = null;

    private volatile CAMQPConnectionStateActor connectionStateActor = null;

    void setConnectionStateActor(CAMQPConnectionStateActor connectionStateActor)
    {
        this.connectionStateActor = connectionStateActor;
    }

    private CAMQPFrameHeader getHeaderAndReset()
    {
        CAMQPFrameHeader currentHeader = header;
        header = null;
        return currentHeader;
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, Channel channel,
            ChannelBuffer buffer)
    {
        if (ctx == null || channel == null || buffer == null)
        {
            throw new IllegalArgumentException("null argument(s) to CAMQPFrameDecoder.decode()");
        }

        if (!connectionStateActor.hasReceivedConnectionHeaderBytes())
        {
            if (buffer.readableBytes() < CAMQPConnectionConstants.HEADER_LENGTH)
            {
                /*
                 * Insufficient bytes available to read the frame header.
                 * Wait until we receive more bytes.
                 */
                return null;
            }

            ChannelBuffer handshakeHeader = buffer.readBytes(CAMQPConnectionConstants.HEADER_LENGTH);
            return  new CAMQPHandshakeFrame(handshakeHeader);
        }

        if (header == null)
        {
            if (buffer.readableBytes() < CAMQPFrameConstants.FRAME_HEADER_SIZE)
            {
                /*
                 * Insufficient bytes available to read the frame header.
                 * Wait until we receive more bytes.
                 */
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
                /*
                 * Insufficient bytes available to read the frame body.
                 * Wait until we receive more bytes.
                 */
                return null;
            }
            int readerIndex = buffer.readerIndex();
            ChannelBuffer body = buffer.slice(readerIndex, frameBodySize);
            buffer.readerIndex(readerIndex + frameBodySize);
            return new CAMQPFrame(getHeaderAndReset(), body);
        }
        else
        {
            /*
             * AMQP frame without a body
             */
            return new CAMQPFrame(getHeaderAndReset(), null);
        }
    }
}