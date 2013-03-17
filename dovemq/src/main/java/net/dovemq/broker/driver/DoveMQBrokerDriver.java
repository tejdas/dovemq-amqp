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

import net.dovemq.broker.endpoint.DoveMQEndpointDriver;

public final class DoveMQBrokerDriver {

    static void shutdown() {
        System.out.println("DoveMQ broker shutting down");
        boolean isBroker = true;
        DoveMQEndpointDriver.shutdown(isBroker);
        System.out.println("DoveMQ broker shut down");
    }

    public static void main(String[] args) {
        boolean isBroker = true;
        DoveMQEndpointDriver.initialize(isBroker, "DoveMQBroker");

        final DoveMQBrokerShutdownHook sh = new DoveMQBrokerShutdownHook();
        Runtime.getRuntime().addShutdownHook(sh);
    }
}
