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

