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

import net.dovemq.transport.endpoint.CAMQPTargetInterface;

public final class RecvEndpoint extends BaseMessageReceiver {
    public RecvEndpoint(String targetEndpointName, CAMQPTargetInterface targetEndpoint) {
        super(targetEndpointName, targetEndpoint);
        this.session = new Session(targetEndpointName, targetEndpoint.getSession());
    }

    public Session getSession() {
        return session;
    }

    private final Session session;
}
