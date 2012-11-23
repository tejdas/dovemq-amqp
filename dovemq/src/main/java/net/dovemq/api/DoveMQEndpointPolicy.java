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

/**
 * Class used to programatically configure a DoveMQ endpoint.
 * @author tejdas
 */
public final class DoveMQEndpointPolicy
{
    /**
     * AUTO: the message is automatically acknowledged by the runtime
     * after it has been delivered to the consumer via
     * {@link DoveMQMessageReceiver#messageReceived()}
     *
     * CONSUMER_ACKS: the message recipient needs to explicitly
     * acknowledge the receipt of the message via
     * {@link Consumer#acknowledge(DoveMQMessage)}
     */
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

    /**
     * Instructs the endpoint to create the Session over
     * a new AMQP connection. By default, it reuses an
     * existing AMQP connection.
     */
    public void createEndpointOnNewConnection()
    {
        this.createSessionOnNewConnection = true;
    }

    public boolean doCreateEndpointOnNewConnection()
    {
        return this.createSessionOnNewConnection;
    }

    private final MessageAcknowledgementPolicy ackPolicy;
    private boolean createSessionOnNewConnection = false;
}
