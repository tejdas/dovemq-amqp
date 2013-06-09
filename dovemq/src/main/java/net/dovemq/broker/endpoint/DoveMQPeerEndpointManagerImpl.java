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

package net.dovemq.broker.endpoint;

import net.dovemq.api.ChannelEndpoint;
import net.dovemq.api.ChannelEndpointListener;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

public class DoveMQPeerEndpointManagerImpl extends DoveMQAbstractEndpointManager {
    private final ChannelEndpointListener endpointListener;
    private final String containerId;

    public DoveMQPeerEndpointManagerImpl(String containerId, ChannelEndpointListener endpointListener) {
        super();
        this.containerId = containerId;
        this.endpointListener = endpointListener;
    }

    @Override
    public void sourceEndpointAttached(String endpointName, CAMQPSourceInterface sourceEndpoint, CAMQPEndpointPolicy endpointPolicy) {
    }

    @Override
    public void sourceEndpointDetached(String endpointName, CAMQPSourceInterface sourceEndpoint, CAMQPEndpointPolicy endpointPolicy) {
    }

    @Override
    public void targetEndpointAttached(final String endpointName, final CAMQPTargetInterface targetEndpoint, CAMQPEndpointPolicy endpointPolicy) {
        ChannelEndpoint channelEndpoint = new ChannelEndpoint(endpointName,
                targetEndpoint);
        endpointListener.channelCreated(channelEndpoint);
    }

    @Override
    public void targetEndpointDetached(String endpointName, CAMQPTargetInterface targetEndpoint, CAMQPEndpointPolicy endpointPolicy) {
    }
}
