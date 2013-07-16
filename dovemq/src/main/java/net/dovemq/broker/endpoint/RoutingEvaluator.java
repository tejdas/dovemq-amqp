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

import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.jcip.annotations.Immutable;

/**
 * This class is used to evaluate an incoming message against
 * a particular route (subscriber) to determine if the message
 * can be routed to the subscriber.
 *
 * This is the base class that is used for {@link TopicRouterType#Basic}
 *
 * @author tejdas
 *
 */
@Immutable
class RoutingEvaluator {
    /**
     * Used to evaluate for {@link TopicRouterType#Basic}.
     *
     * @param routingEvaluationContext
     * @return true
     */
    boolean canMessageBePublished(Object routingEvaluationContext) {
        return true;
    }

    CAMQPSourceInterface getSubscriberProxy() {
        return subscriberProxy;
    }

    long getTimeOfSubscription() {
        return timeOfSubscription;
    }

    public RoutingEvaluator(CAMQPSourceInterface subscriberProxy) {
        super();
        this.subscriberProxy = subscriberProxy;
        timeOfSubscription = System.currentTimeMillis();
    }

    private final CAMQPSourceInterface subscriberProxy;
    private final long timeOfSubscription;
}
