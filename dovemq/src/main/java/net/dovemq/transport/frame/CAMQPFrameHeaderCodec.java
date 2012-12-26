/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.frame;

import net.dovemq.transport.protocol.CAMQPCodecUtil;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * Encoder/Decoder of AMQP frame header
 * @author tejdas
 *
 */
public final class CAMQPFrameHeaderCodec {
    public static ChannelBuffer encode(CAMQPFrameHeader header) {
        int variableHeaderSize = (header.getDataOffset() - CAMQPFrameConstants.DEFAULT_DATA_OFFSET) * 4;
        ChannelBuffer headerBuffer = ChannelBuffers.dynamicBuffer(CAMQPFrameConstants.FRAME_HEADER_SIZE + variableHeaderSize);

        CAMQPCodecUtil.writeUInt(header.getFrameSize(), headerBuffer);
        CAMQPCodecUtil.writeUByte(header.getDataOffset(), headerBuffer);
        CAMQPCodecUtil.writeUByte(header.getFrameType(), headerBuffer);
        CAMQPCodecUtil.writeUShort(header.getChannelNumber(), headerBuffer);
        headerBuffer.writerIndex(header.getDataOffset() * 4);

        return headerBuffer;
    }

    public static CAMQPFrameHeader decode(ChannelBuffer buffer) {
        CAMQPFrameHeader frameHeader = new CAMQPFrameHeader();

        long frameSize = CAMQPCodecUtil.readUInt(buffer);
        frameHeader.setFrameSize(frameSize);

        short dataOffset = (short) CAMQPCodecUtil.readUByte(buffer);
        frameHeader.setDataOffset(dataOffset);

        short frameType = (short) CAMQPCodecUtil.readUByte(buffer);
        frameHeader.setFrameType(frameType);

        short channelNumber = (short) CAMQPCodecUtil.readUShort(buffer);
        frameHeader.setChannelNumber(channelNumber);

        if (dataOffset > CAMQPFrameConstants.DEFAULT_DATA_OFFSET) {
            buffer.skipBytes((dataOffset - CAMQPFrameConstants.DEFAULT_DATA_OFFSET) * 4);
        }
        return frameHeader;
    }
}
