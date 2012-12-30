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

import org.apache.commons.lang.StringUtils;

/**
 * This class is used to evaluate an incoming message against
 * a particular route (subscriber) to determine if the message
 * can be routed to the subscriber.
 *
 * This is the base class that is used for {@link TopicRouterType#Hierarchical}
 *
 * @author tejdas
 *
 */
class RoutingTopicHeirarchyMatcher extends RoutingEvaluator {
    /**
     * Evaluate the routingEvaluationContext (topicPublishHierarchy) of the
     * incoming message against the Subscriber's topic hierarchy to determine if
     * the message could be routed to the subscriber.
     */
    @Override
    boolean canMessageBePublished(Object routingEvaluationContext) {
        String publisherTopicHierarchy = (String) routingEvaluationContext;
        if (StringUtils.isEmpty(publisherTopicHierarchy)) {
            return false;
        }

        if (subscriberTopicHierarchy.equalsIgnoreCase(publisherTopicHierarchy)) {
            return true;
        }

        String dotSuffixedSubscriberTopicHierarchy = String.format("%s.", subscriberTopicHierarchy);
        return (publisherTopicHierarchy.startsWith(dotSuffixedSubscriberTopicHierarchy));
    }

    public RoutingTopicHeirarchyMatcher(String subscriberTopicHierarchy,
            CAMQPSourceInterface subscriberProxy) {
        super(subscriberProxy);
        this.subscriberTopicHierarchy = subscriberTopicHierarchy;
    }

    private final String subscriberTopicHierarchy;
}
