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

package net.dovemq.api;

import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.session.CAMQPSessionFactory;
import net.dovemq.transport.session.CAMQPSessionInterface;

public final class ConnectionFactory
{
    private volatile String endpointId = null;
    public String getEndpointId()
    {
        return endpointId;
    }

    /**
     * Initialize DoveMQ runtime with a supplied endpointId.
     * The endpointId allows the runtime to be distinguishable
     * from other DoveMQ endpoints on the same machine, and also
     * uniquely addressable.
     *
     * @param endpointID
     */
    public void initialize(String endpointID)
    {
        if (endpointId != null)
        {
            boolean isBroker = false;
            CAMQPLinkManager.initialize(isBroker, endpointID);
            endpointId = CAMQPConnectionManager.getContainerId();
        }
    }

    /**
     * Shuts down DoveMQ runtime.
     */
    public void shutdown()
    {
        if (endpointId != null)
        {
            CAMQPLinkManager.shutdown();
        }
    }

    public Session createSession(String targetDoveMQBrokerAddress)
    {
        String brokerContainerId = String.format("broker@%s", targetDoveMQBrokerAddress);
        CAMQPSessionInterface camqpSession = CAMQPSessionFactory.createCAMQPSession(brokerContainerId);
        return new Session(brokerContainerId, camqpSession);
    }
}
