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

package net.dovemq.broker.driver;

import net.dovemq.broker.endpoint.DoveMQEndpointManager;
import net.dovemq.broker.endpoint.DoveMQEndpointManagerImpl;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.ReceiverLinkCreditPolicy;

public class DoveMQBrokerDriver
{
    private static volatile boolean doShutdown = false;

    static void shutdown()
    {
        System.out.println("DoveMQ broker shutting down");
        CAMQPLinkManager.shutdown();
        System.out.println("DoveMQ broker shut down");
        doShutdown = true;
    }

    public static void main(String[] args)
    {
        final DoveMQBrokerShutdownHook sh = new DoveMQBrokerShutdownHook();
        Runtime.getRuntime().addShutdownHook(sh);

        CAMQPLinkManager.initialize(true, "broker");

        CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();
        defaultEndpointPolicy.setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);
        CAMQPEndpointManager.setDefaultEndpointPolicy(defaultEndpointPolicy);

        DoveMQEndpointManager doveMQEndpointManager = new DoveMQEndpointManagerImpl();
        CAMQPEndpointManager.registerDoveMQEndpointManager(doveMQEndpointManager);
        while (!doShutdown)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
