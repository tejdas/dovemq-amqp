package net.dovemq.transport.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

class CAMQPSyncBinaryDataParser
{
    static ChannelBuffer parseBinaryData(ChannelBuffer buffer, long size, boolean copyFree)
    {
        if (!buffer.readable())
        {
            return null;
        }

        int toRead = (size > (CAMQPProtocolConstants.USHORT_MAX_VALUE + 1)) ? (CAMQPProtocolConstants.USHORT_MAX_VALUE + 1) : (int) size;
        int readableBytes = buffer.readableBytes();
        if (toRead <= readableBytes)
        {
            return readBuffer(buffer, toRead, copyFree);
        }
        else
        {
            return readBuffer(buffer, readableBytes, copyFree);
        }
    }

    private static ChannelBuffer readBuffer(ChannelBuffer buffer, int bytesToRead, boolean copyFree)
    {
        ChannelBuffer binaryData;
        if (copyFree)
        {
            int readerIndex = buffer.readerIndex();
            binaryData = buffer.slice(readerIndex, bytesToRead);
            buffer.readerIndex(readerIndex + bytesToRead);
        }
        else
        {
            binaryData = ChannelBuffers.buffer(CAMQPProtocolConstants.USHORT_MAX_VALUE + 1);
            buffer.readBytes(binaryData, bytesToRead);
            buffer.discardReadBytes();
        }
        return binaryData;
    }
}
