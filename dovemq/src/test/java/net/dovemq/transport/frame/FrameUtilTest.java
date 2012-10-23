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
import junit.framework.TestCase;

public class FrameUtilTest extends TestCase
{

    public FrameUtilTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testCAMQPFrameCodec()
    {
        CAMQPFrameHeader inputFrameHeader = new CAMQPFrameHeader();
        inputFrameHeader.setChannelNumber((short) 12);
        inputFrameHeader.setFrameSize(65535);
        short dataOffset = 4;
        inputFrameHeader.setDataOffset(dataOffset);

        ChannelBuffer buffer = CAMQPFrameHeaderCodec.encode(inputFrameHeader);
        assertTrue(buffer.readableBytes() >= CAMQPFrameConstants.FRAME_HEADER_SIZE);
        CAMQPFrameHeader outputFrameHeader =
                CAMQPFrameHeaderCodec.decode(buffer);
        assertTrue(compareFrames(inputFrameHeader, outputFrameHeader));
    }
    
    private boolean compareFrames(CAMQPFrameHeader in, CAMQPFrameHeader out)
    {
        return ((in.getChannelNumber() == out.getChannelNumber()) &&
                (in.getDataOffset() == out.getDataOffset())) &&
                (out.getDataOffset() > CAMQPFrameConstants.DEFAULT_DATA_OFFSET) &&
                (in.getFrameSize() == out.getFrameSize());
    }
}


