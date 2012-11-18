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

public class DoveMQEndpointPolicy
{
    public static enum MessageAcknowledgementPolicy
    {
        AUTO,
        CONSUMER_ACKS,
    }

    public DoveMQEndpointPolicy()
    {
        super();
        this.ackPolicy = MessageAcknowledgementPolicy.AUTO;
    }

    public DoveMQEndpointPolicy(MessageAcknowledgementPolicy ackPolicy)
    {
        super();
        this.ackPolicy = ackPolicy;
    }

    public DoveMQEndpointPolicy(DoveMQEndpointPolicy that)
    {
        super();
        this.ackPolicy = that.ackPolicy;
    }

    public MessageAcknowledgementPolicy getAckPolicy()
    {
        return ackPolicy;
    }

    public void createEndpointOnNewConnection()
    {
        this.createConnection = true;
    }

    public boolean doCreateEndpointOnNewConnection()
    {
        return this.createConnection;
    }

    private final MessageAcknowledgementPolicy ackPolicy;
    private boolean createConnection = false;
}
