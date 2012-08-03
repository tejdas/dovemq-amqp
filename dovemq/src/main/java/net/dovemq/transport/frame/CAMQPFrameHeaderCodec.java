package net.dovemq.transport.frame;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import net.dovemq.transport.protocol.CAMQPCodecUtil;

public class CAMQPFrameHeaderCodec
{
    public static ChannelBuffer encode(CAMQPFrameHeader header)
    {
        int variableHeaderSize = (header.getDataOffset() - CAMQPFrameConstants.DEFAULT_DATA_OFFSET) * 4;
        ChannelBuffer headerBuffer = ChannelBuffers.dynamicBuffer(CAMQPFrameConstants.FRAME_HEADER_SIZE + variableHeaderSize);

        CAMQPCodecUtil.writeUInt(header.getFrameSize(), headerBuffer);
        CAMQPCodecUtil.writeUByte(header.getDataOffset(), headerBuffer);
        CAMQPCodecUtil.writeUByte(header.getFrameType(), headerBuffer);
        CAMQPCodecUtil.writeUShort(header.getChannelNumber(), headerBuffer);
        headerBuffer.writerIndex(header.getDataOffset() * 4);

        return headerBuffer;
    }

    public static CAMQPFrameHeader decode(ChannelBuffer buffer)
    {
        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();

        long frameSize = CAMQPCodecUtil.readUInt(buffer);
        frameHeader.setFrameSize(frameSize);

        short dataOffset = (short) CAMQPCodecUtil.readUByte(buffer);
        frameHeader.setDataOffset(dataOffset);

        short frameType = (short) CAMQPCodecUtil.readUByte(buffer);
        frameHeader.setFrameType(frameType);

        short channelNumber = (short) CAMQPCodecUtil.readUShort(buffer);
        frameHeader.setChannelNumber(channelNumber);

        if (dataOffset > CAMQPFrameConstants.DEFAULT_DATA_OFFSET)
        {
            buffer.skipBytes((dataOffset - CAMQPFrameConstants.DEFAULT_DATA_OFFSET) * 4);
        }
        return frameHeader;
    }
}
