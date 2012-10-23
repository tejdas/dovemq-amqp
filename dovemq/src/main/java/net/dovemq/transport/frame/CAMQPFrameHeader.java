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

    public short getDataOffset()
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
