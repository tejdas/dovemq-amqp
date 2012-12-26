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

package net.dovemq.transport.endpoint;

import java.util.HashMap;
import java.util.Map;

import net.dovemq.api.DoveMQEndpointPolicy;
import net.dovemq.broker.endpoint.TopicRouterType;
import net.dovemq.transport.link.CAMQPLinkConstants;
import net.dovemq.transport.link.ReceiverLinkCreditPolicy;
import net.dovemq.transport.protocol.data.CAMQPConstants;

import org.apache.commons.lang.StringUtils;

public final class CAMQPEndpointPolicy
{
    /**
     * Following key is used by publisher/subscriber end-points to indicate the TopicRouter type.
     * See {@link TopicRouterType}
     */
    private static final String TOPIC_ROUTER_TYPE_KEY = "TopicRouterTypeKey";
    /**
     * Following key is used by a subscriber of type {@link TopicRouterType#MessageTagFilter}
     * to indicate its message filter regex pattern.
     */
    private static final String MESSAGE_FILTER_PATTERN_KEY = "MessageFilterPatternKey";

    public static enum CAMQPMessageDeliveryPolicy
    {
        AtleastOnce,
        AtmostOnce,
        ExactlyOnce
    }

    public static enum EndpointType
    {
        QUEUE,
        TOPIC
    }

    private final long maxMessageSize;
    private final long maxAvailableLimit;
    private final int senderSettleMode;
    private final int receiverSettleMode;
    private final long minLinkCreditThreshold;
    private final long linkCreditBoost;
    private EndpointType endpointType = EndpointType.QUEUE;
    private final CAMQPMessageDeliveryPolicy deliveryPolicy;
    private ReceiverLinkCreditPolicy linkCreditPolicy;
    private String subscriptionTopicHierarchy;
    private DoveMQEndpointPolicy doveMQEndpointPolicy = new DoveMQEndpointPolicy();
    private final Map<String, String> customProperties = new HashMap<String, String>();

    public String getSubscriptionTopicHierarchy()
    {
        return subscriptionTopicHierarchy;
    }

    public void setSubscriptionTopicHierarchy(String subscriptionTopicHierarchy)
    {
        this.subscriptionTopicHierarchy = subscriptionTopicHierarchy;
    }

    public Map<String, String> getCustomProperties()
    {
        return customProperties;
    }

    public String getMessageFilterPattern()
    {
        return customProperties.get(MESSAGE_FILTER_PATTERN_KEY);
    }

    public void setMessageFilterPattern(String messageFilterPattern)
    {
        customProperties.put(MESSAGE_FILTER_PATTERN_KEY,  messageFilterPattern);
    }

    public void setTopicRouterType(TopicRouterType topicType)
    {
        customProperties.put(TOPIC_ROUTER_TYPE_KEY,  topicType.name());
    }

    public TopicRouterType getTopicRouterType()
    {
        String topicTypeString = customProperties.get(TOPIC_ROUTER_TYPE_KEY);
        if (StringUtils.isEmpty(topicTypeString))
        {
            return TopicRouterType.Basic;
        }
        else
        {
            return TopicRouterType.valueOf(topicTypeString);
        }
    }

    public DoveMQEndpointPolicy getDoveMQEndpointPolicy()
    {
        return doveMQEndpointPolicy;
    }

    public void setDoveMQEndpointPolicy(DoveMQEndpointPolicy doveMQEndpointPolicy)
    {
        this.doveMQEndpointPolicy = doveMQEndpointPolicy;
    }

    public void setLinkCreditPolicy(ReceiverLinkCreditPolicy linkCreditPolicy)
    {
        this.linkCreditPolicy = linkCreditPolicy;
    }

    public EndpointType getEndpointType()
    {
        return endpointType;
    }

    public void setEndpointType(EndpointType endpointType)
    {
        this.endpointType = endpointType;
    }

    public CAMQPMessageDeliveryPolicy getDeliveryPolicy()
    {
        return deliveryPolicy;
    }

    public long getMaxMessageSize()
    {
        return maxMessageSize;
    }

    public long getMaxAvailableLimit()
    {
        return maxAvailableLimit;
    }

    public int getSenderSettleMode()
    {
        return senderSettleMode;
    }

    public int getReceiverSettleMode()
    {
        return receiverSettleMode;
    }

    public ReceiverLinkCreditPolicy getLinkCreditPolicy()
    {
        return linkCreditPolicy;
    }

    public long getMinLinkCreditThreshold()
    {
        return minLinkCreditThreshold;
    }

    public long getLinkCreditBoost()
    {
        return linkCreditBoost;
    }

    public CAMQPEndpointPolicy(long maxMessageSize,
            long maxAvailableLimit,
            String senderSettleMode,
            String receiverSettleMode,
            CAMQPMessageDeliveryPolicy deliveryPolicy,
            ReceiverLinkCreditPolicy linkCreditPolicy,
            long minLinkCreditThreshold,
            long linkCreditBoost)
    {
        super();
        this.maxMessageSize = maxMessageSize;
        this.maxAvailableLimit = maxAvailableLimit;
        this.deliveryPolicy = deliveryPolicy;
        this.linkCreditPolicy = linkCreditPolicy;
        this.minLinkCreditThreshold = minLinkCreditThreshold;
        this.linkCreditBoost = linkCreditBoost;

        if (senderSettleMode.equalsIgnoreCase(CAMQPConstants.SENDER_SETTLE_MODE_SETTLED_STR))
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_SETTLED;
        else if (senderSettleMode.equalsIgnoreCase(CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED_STR))
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
        else
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED;

        if (receiverSettleMode.equalsIgnoreCase(CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST_STR))
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
        else
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
    }

    public CAMQPEndpointPolicy(long maxMessageSize,
            int senderSettleMode,
            int receiverSettleMode,
            CAMQPEndpointPolicy that)
    {
        super();
        this.maxMessageSize = maxMessageSize;
        this.maxAvailableLimit = that.maxAvailableLimit;
        this.senderSettleMode = senderSettleMode;
        this.receiverSettleMode = receiverSettleMode;
        linkCreditPolicy = that.linkCreditPolicy;
        minLinkCreditThreshold = that.minLinkCreditThreshold;
        linkCreditBoost = that.linkCreditBoost;

        if ((senderSettleMode == CAMQPConstants.SENDER_SETTLE_MODE_SETTLED) &&
            (receiverSettleMode == CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST))
        {
            deliveryPolicy = CAMQPMessageDeliveryPolicy.AtmostOnce;
        }
        else if ((senderSettleMode == CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED) &&
                 (receiverSettleMode == CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST))
        {
            deliveryPolicy = CAMQPMessageDeliveryPolicy.AtleastOnce;
        }
        else
        {
            deliveryPolicy = that.deliveryPolicy;
        }

        doveMQEndpointPolicy = new DoveMQEndpointPolicy(that.doveMQEndpointPolicy);
    }

    public CAMQPEndpointPolicy(CAMQPMessageDeliveryPolicy deliveryPolicy)
    {
        super();
        this.maxMessageSize = CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE;
        this.maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
        linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
        minLinkCreditThreshold = 10;
        linkCreditBoost = 100;
        this.deliveryPolicy = deliveryPolicy;
        switch (deliveryPolicy)
        {
        case AtmostOnce:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_SETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
            break;

        case AtleastOnce:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
            break;

        case ExactlyOnce:
        default:
            this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_UNSETTLED;
            this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
            break;
        }
    }

    public CAMQPEndpointPolicy()
    {
        super();
        this.maxMessageSize = CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE;
        this.maxAvailableLimit = CAMQPLinkConstants.DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER;
        this.senderSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED;
        this.receiverSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
        deliveryPolicy = CAMQPMessageDeliveryPolicy.ExactlyOnce;
        linkCreditPolicy = ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE;
        minLinkCreditThreshold = 128;
        linkCreditBoost = 1024;
    }
}
