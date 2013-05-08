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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.utils.CAMQPThreadFactory;

public abstract class DoveMQAbstractEndpointManager implements DoveMQEndpointManager {
    private final ExecutorService executor = Executors.newCachedThreadPool(new CAMQPThreadFactory("DoveMQEndpointManagerThread"));

    ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(300, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
