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

import java.util.concurrent.ExecutorService;

import net.dovemq.api.DoveMQEndpointPolicy;
import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;
import net.dovemq.api.RecvEndpointListener;
import net.dovemq.transport.connection.CAMQPConnectionConstants;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.ReceiverLinkCreditPolicy;

public final class DoveMQEndpointDriver {
    private static volatile DoveMQAbstractEndpointManager manager = null;

    public static void initialize(String containerId) {
        CAMQPLinkManager.initialize(containerId);
    }

    public static void initializeBrokerEndpoint(String containerId) {
        CAMQPLinkManager.initializeEndpoint(CAMQPConnectionConstants.AMQP_IANA_PORT, containerId);

        CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();
        defaultEndpointPolicy
                .setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);
        defaultEndpointPolicy.setDoveMQEndpointPolicy(new DoveMQEndpointPolicy(
                MessageAcknowledgementPolicy.CONSUMER_ACKS));

        CAMQPEndpointManager.setDefaultEndpointPolicy(defaultEndpointPolicy);

        manager = new DoveMQBrokerEndpointManagerImpl();
        CAMQPEndpointManager.registerDoveMQEndpointManager(manager);
    }

    public static void initializeEndpoint(int listenerPort, String containerId, RecvEndpointListener endpointListener) {
        CAMQPLinkManager.initializeEndpoint(listenerPort, containerId);

        CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();
        defaultEndpointPolicy
                .setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);
        CAMQPEndpointManager.setDefaultEndpointPolicy(defaultEndpointPolicy);

        manager = new DoveMQPeerEndpointManagerImpl(containerId, endpointListener);
        CAMQPEndpointManager.registerDoveMQEndpointManager(manager);
    }

    public static void initializeClientEndpoint(String containerId, RecvEndpointListener endpointListener) {
        CAMQPLinkManager.initialize(containerId);

        CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();
        defaultEndpointPolicy
                .setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);
        CAMQPEndpointManager.setDefaultEndpointPolicy(defaultEndpointPolicy);

        manager = new DoveMQPeerEndpointManagerImpl(containerId,
                endpointListener);
        CAMQPEndpointManager.registerDoveMQEndpointManager(manager);
    }

    public static void shutdown(boolean isBroker) {
        if (isBroker) {
            shutdownManager();
        }
        CAMQPLinkManager.shutdown();
    }


    static ExecutorService getExecutor() {
        if (manager == null) {
            throw new IllegalStateException("DoveMQEndpointManager has not been initialized yet");
        }
        return manager.getExecutor();
    }

    /*
     * For junit testing
     */
    static void setManager(DoveMQBrokerEndpointManagerImpl manager) {
        DoveMQEndpointDriver.manager = manager;
    }

    static void shutdownManager() {
        if (manager == null) {
            throw new IllegalStateException("DoveMQEndpointManager has not been initialized yet");
        }
        manager.shutdown();
    }
}
