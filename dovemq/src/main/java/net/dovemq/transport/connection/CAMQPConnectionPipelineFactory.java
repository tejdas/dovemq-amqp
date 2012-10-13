package net.dovemq.transport.connection;

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;


class CAMQPConnectionPipelineFactory implements ChannelPipelineFactory
{
    private final boolean isInitiator;
    private final CAMQPConnectionProperties connectionProps;
    CAMQPConnectionPipelineFactory(boolean isInitiator, CAMQPConnectionProperties connectionProps)
    {
        super();
        this.isInitiator = isInitiator;
        this.connectionProps = connectionProps;
    }

    @Override
    public ChannelPipeline getPipeline()
    {
        ChannelPipeline p = pipeline();
        CAMQPConnectionProperties clonedConnectionProps = (connectionProps != null)? connectionProps.cloneProperties() : null;
        ChannelHandler decoderAndDispatcher = new CAMQPConnectionHandler(isInitiator, clonedConnectionProps);
        p.addLast("connectionHeaderDecoder", decoderAndDispatcher);
        p.addLast("FrameDecoder", new CAMQPFrameDecoder());
        p.addLast("FrameDispatcher", decoderAndDispatcher);        
        return p;
    }
}

