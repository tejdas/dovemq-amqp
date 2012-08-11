package net.dovemq.transport.frame;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * In-memory representation of AMQP frame header
 * @author tejdas
 *
 */
public class CAMQPFrameHeader
{
    public static CAMQPFrameHeader createFrameHeader(int channelNumber, int frameBodySize)
    {
        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();
        frameHeader.setChannelNumber((short) channelNumber);
        frameHeader.setFrameSize(CAMQPFrameConstants.FRAME_HEADER_SIZE + frameBodySize);
        return frameHeader;
    }

    public static ChannelBuffer createEncodedFrameHeader(int channelNumber, int frameBodySize)
    {
        CAMQPFrameHeader frameHeader = createFrameHeader(channelNumber, frameBodySize);
        return CAMQPFrameHeaderCodec.encode(frameHeader);
    }

    public CAMQPFrameHeader()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    public long getFrameSize()
    {
        return frameSize;
    }

    public void setFrameSize(long frameSize)
    {
        this.frameSize = frameSize;
    }

    public short getChannelNumber()
    {
        return channelNumber;
    }

    public void setChannelNumber(short channelNumber)
    {
        this.channelNumber = channelNumber;
    }

    short getDataOffset()
    {
        return dataOffset;
    }

    void setDataOffset(short dataOffset)
    {
        this.dataOffset = dataOffset;
    }

    public int getFrameType()
    {
        return frameType;
    }

    public void setFrameType(int frameType)
    {
        this.frameType = frameType;
    }

    private short channelNumber = 0;

    private long frameSize = 0;

    private int frameType = CAMQPFrameConstants.AMQP_FRAME_TYPE;

    private short dataOffset = CAMQPFrameConstants.DEFAULT_DATA_OFFSET;
}
