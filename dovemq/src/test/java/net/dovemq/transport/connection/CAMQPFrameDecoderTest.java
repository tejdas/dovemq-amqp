package net.dovemq.transport.connection;

import junit.framework.TestCase;
import net.dovemq.transport.connection.CAMQPConnectionStateActor;
import net.dovemq.transport.connection.CAMQPFrameDecoder;
import net.dovemq.transport.connection.mockjetty.MockJettyChannel;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPFrameHeaderCodec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

public class CAMQPFrameDecoderTest extends TestCase
{
    private Mockery mockContext = null;

    public CAMQPFrameDecoderTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        mockContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testDecodeInsufficientHeader() throws Exception
    {
        final ChannelHandlerContext ctx = mockContext.mock(ChannelHandlerContext.class);
        final CAMQPConnectionStateActor mockActor = mockContext.mock(CAMQPConnectionStateActor.class);
        mockContext.checking(new Expectations() {
            {
                ignoring(mockActor).hasReceivedConnectionHeaderBytes();will(returnValue(true));
            }
        });

        Channel channel = new MockJettyChannel(false);
        CAMQPFrameDecoder decoder = new CAMQPFrameDecoder();
        decoder.setConnectionStateActor(mockActor);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(CAMQPFrameConstants.FRAME_HEADER_SIZE - 2);
        assertEquals(null, decoder.decode(ctx, channel, buffer));
    }

    @Test
    public void testDecodeOneFrame() throws Exception
    {
        final ChannelHandlerContext ctx = mockContext.mock(ChannelHandlerContext.class);
        Channel channel = new MockJettyChannel(false);
        final CAMQPConnectionStateActor mockActor = mockContext.mock(CAMQPConnectionStateActor.class);
        mockContext.checking(new Expectations() {
            {
                ignoring(mockActor).hasReceivedConnectionHeaderBytes();will(returnValue(true));
            }
        });

        int frameSize = 1024;
        CAMQPFrameDecoder decoder = new CAMQPFrameDecoder();
        decoder.setConnectionStateActor(mockActor);
        CAMQPFrameHeader inputFrameHeader = new CAMQPFrameHeader();
        inputFrameHeader.setChannelNumber((short) 12);
        inputFrameHeader.setFrameSize(frameSize);

        ChannelBuffer headerBuffer =
                CAMQPFrameHeaderCodec.encode(inputFrameHeader);
        ChannelBuffer bodyBuffer =
                ChannelBuffers.dynamicBuffer(frameSize
                        - CAMQPFrameConstants.FRAME_HEADER_SIZE);
        byte[] body =
                new byte[frameSize - CAMQPFrameConstants.FRAME_HEADER_SIZE];
        bodyBuffer.writeBytes(body);
        ChannelBuffer frameBuffer =
                ChannelBuffers.wrappedBuffer(headerBuffer, bodyBuffer);

        Object object = decoder.decode(ctx, channel, frameBuffer);
        assertTrue(object != null);
        assertTrue(object instanceof CAMQPFrame);
        CAMQPFrame decodedFrame = (CAMQPFrame) object;
        CAMQPFrameHeader outputFrameHeader = decodedFrame.getHeader();
        assertEquals(outputFrameHeader.getChannelNumber(), inputFrameHeader
                .getChannelNumber());
        assertEquals(outputFrameHeader.getFrameSize(), frameSize);
        ChannelBuffer outputBodyBuffer = decodedFrame.getBody();
        assertEquals(outputBodyBuffer.readableBytes(), frameSize
                - CAMQPFrameConstants.FRAME_HEADER_SIZE);
    }

    @Test
    public void testDecodeOneFrameMultipleReads() throws Exception
    {
        final ChannelHandlerContext ctx = mockContext.mock(ChannelHandlerContext.class);
        Channel channel = new MockJettyChannel(false);
        final CAMQPConnectionStateActor mockActor = mockContext.mock(CAMQPConnectionStateActor.class);
        mockContext.checking(new Expectations() {
            {
                ignoring(mockActor).hasReceivedConnectionHeaderBytes();will(returnValue(true));
            }
        });

        int frameSize = 1024;
        CAMQPFrameDecoder decoder = new CAMQPFrameDecoder();
        decoder.setConnectionStateActor(mockActor);
        CAMQPFrameHeader inputFrameHeader = new CAMQPFrameHeader();
        inputFrameHeader.setChannelNumber((short) 12);
        inputFrameHeader.setFrameSize(frameSize);

        ChannelBuffer headerBuffer =
                CAMQPFrameHeaderCodec.encode(inputFrameHeader);
        ChannelBuffer bodyBuffer =
                ChannelBuffers.dynamicBuffer(frameSize
                        - CAMQPFrameConstants.FRAME_HEADER_SIZE);
        byte[] body =
                new byte[frameSize - CAMQPFrameConstants.FRAME_HEADER_SIZE];
        bodyBuffer.writeBytes(body);
        ChannelBuffer frameBuffer =
                ChannelBuffers.wrappedBuffer(headerBuffer, bodyBuffer);

        ChannelBuffer partialFrame = ChannelBuffers.buffer(384);
        frameBuffer.readBytes(partialFrame, 384);

        assertEquals(null, decoder.decode(ctx, channel, partialFrame));

        ChannelBuffer remainingBodyBuffer = ChannelBuffers.wrappedBuffer(partialFrame, frameBuffer);
        Object object = decoder.decode(ctx, channel, remainingBodyBuffer);
        assertTrue(object != null);
        assertTrue(object instanceof CAMQPFrame);
        CAMQPFrame decodedFrame = (CAMQPFrame) object;
        CAMQPFrameHeader outputFrameHeader = decodedFrame.getHeader();
        assertEquals(outputFrameHeader.getChannelNumber(), inputFrameHeader
                .getChannelNumber());
        assertEquals(outputFrameHeader.getFrameSize(), frameSize);
        ChannelBuffer outputBodyBuffer = decodedFrame.getBody();
        assertEquals(outputBodyBuffer.readableBytes(), frameSize
                - CAMQPFrameConstants.FRAME_HEADER_SIZE);
    }
}
