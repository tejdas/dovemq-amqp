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


