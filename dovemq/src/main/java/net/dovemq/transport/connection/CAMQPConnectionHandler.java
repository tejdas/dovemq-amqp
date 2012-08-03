package net.dovemq.transport.connection;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlClose;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;

class CAMQPConnectionHandler extends SimpleChannelUpstreamHandler
{
    private static final Logger log = Logger.getLogger(CAMQPConnectionHandler.class);

    private final CAMQPConnectionStateActor stateActor;

    private CAMQPConnection connection = null;

    void registerConnection(CAMQPConnection connection)
    {
        this.connection = connection;
    }

    CAMQPConnectionStateActor getStateActor()
    {
        return stateActor;
    }

    CAMQPConnectionHandler(boolean isInitiator, CAMQPConnectionProperties connectionProps)
    {
        stateActor = new CAMQPConnectionStateActor(isInitiator, connectionProps);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        Channel channel = ctx.getChannel();
        assert (channel != null);
        stateActor.setChannel(channel);
        ctx.sendUpstream(e);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        stateActor.receivedDisconnect();
        super.channelDisconnected(ctx, e);
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception
    {
        if (stateActor.isConnectionHandshakeInProgress())
        {
            if (e instanceof MessageEvent)
            {
                Object message = ((MessageEvent) e).getMessage();
                if (!(message instanceof ChannelBuffer))
                {
                    ctx.sendUpstream(e);
                    return;
                }
                stateActor.receivedConnectionHeaderBytes((ChannelBuffer) message);
            }
        }
        super.handleUpstream(ctx, e);
    }

    /**
     * Invoked when an exception was raised by an I/O thread or a
     * {@link ChannelHandler}.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        log.warn("exceptionCaught: " + e.getCause().getMessage());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e)
    {
        Object message = e.getMessage();
        if (!(message instanceof CAMQPFrame))
        {
            ctx.sendUpstream(e);
        }
        else
        {
            CAMQPFrame frame = (CAMQPFrame) e.getMessage();
            frameReceived(frame);
        }
    }

    private void frameReceived(CAMQPFrame frame)
    {
        ChannelBuffer frameBody = frame.getBody();
        if (frameBody == null)
        {
            /*
             * Heart-Beat control
             */
            stateActor.receivedHeartbeat();
            return;
        }

        CAMQPFrameHeader frameHeader = frame.getHeader();
        int channelNumber = frameHeader.getChannelNumber();
        if (channelNumber == 0)
        {
            /*
             * connection frame
             */
            CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
            decoder.take(frameBody);
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPControlOpen.descriptor))
            {
                CAMQPControlOpen peerConnectionProps = CAMQPControlOpen.decode(decoder);
                stateActor.receivedOpenControl(peerConnectionProps);
            }
            else if (controlName.equalsIgnoreCase(CAMQPControlClose.descriptor))
            {
                CAMQPControlClose closeContext = CAMQPControlClose.decode(decoder);
                stateActor.receivedCloseControl(closeContext);
            }
        }

        else
        {
            connection.frameReceived(channelNumber, frame);
        }
    }
}
