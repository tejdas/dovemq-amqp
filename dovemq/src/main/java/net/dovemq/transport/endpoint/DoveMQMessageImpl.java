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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.HeaderProperties;
import net.dovemq.api.MessageProperties;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPProtocolConstants;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.jcip.annotations.Immutable;

import org.apache.commons.lang.StringUtils;

public class DoveMQMessageImpl implements DoveMQMessage
{
    private static final String SYMBOL_DELIVERY_ANNOTATIONS = "amqp:delivery-annotations:map";
    private static final String SYMBOL_MESSAGE_ANNOTATIONS = "amqp:message-annotations:map";
    private static final String SYMBOL_APPLICATION_ANNOTATIONS = "amqp:application-annotations:map";
    private static final String SYMBOL_FOOTERS = "amqp:footer:map";
    public static final String ROUTING_TAG_KEY = "RoutingTagKey";
    public static final String TOPIC_PUBLISH_HIERARCHY_KEY = "TopicPublishHierarchyKey";

    @Immutable
    private static class DoveMQPayload
    {
        public DoveMQPayload(byte[] payload)
        {
            super();
            this.payload = Arrays.copyOf(payload, payload.length);
        }

        void encode(CAMQPEncoder encoder)
        {
            encoder.writeBinaryPayload(payload, payload.length);
        }

        byte[] getPayload()
        {
            return payload;
        }

        private final byte[] payload;
    }

    @Override
    public HeaderProperties getHeaderProperties()
    {
        return headerProperties;
    }

    @Override
    public void addDeliveryAnnotation(String key, String val)
    {
        if (deliveryAnnotations == null)
        {
            deliveryAnnotations = new HashMap<String, String>();
        }
        deliveryAnnotations.put(key,  val);
    }

    @Override
    public void addMessageAnnotation(String key, String val)
    {
        if (messageAnnotations == null)
        {
            messageAnnotations = new HashMap<String, String>();
        }
        messageAnnotations.put(key,  val);
    }

    @Override
    public MessageProperties getMessageProperties()
    {
        return messageProperties;
    }

    @Override
    public void addApplicationProperty(String key, String val)
    {
        if (applicationProperties == null)
        {
            applicationProperties = new HashMap<String, String>();
        }
        applicationProperties.put(key,  val);
    }

    @Override
    public void addPayload(byte[] body)
    {
        if (payloads != null)
        {
            payloads.add(new DoveMQPayload(body));
        }
        else if (payload != null)
        {
            payloads = new ArrayList<DoveMQPayload>();
            payloads.add(new DoveMQPayload(body));
        }
        else
        {
            payload = new DoveMQPayload(body);
        }
    }

    @Override
    public void addFooter(String key, String val)
    {
        if (footers == null)
        {
            footers = new HashMap<String, String>();
        }
        footers.put(key,  val);
    }

    @Override
    public String getDeliveryAnnotation(String key)
    {
        if (deliveryAnnotations != null)
        {
            return deliveryAnnotations.get(key);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Collection<String> getDeliveryAnnotationKeys()
    {
        if (deliveryAnnotations != null)
        {
            return deliveryAnnotations.keySet();
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getMessageAnnotation(String key)
    {
        if (messageAnnotations != null)
        {
            return messageAnnotations.get(key);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Collection<String> getMessageAnnotationKeys()
    {
        if (messageAnnotations != null)
        {
            return messageAnnotations.keySet();
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getApplicationProperty(String key)
    {
        if (applicationProperties != null)
        {
            return applicationProperties.get(key);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Collection<String> getApplicationPropertyKeys()
    {
        if (applicationProperties != null)
        {
            return applicationProperties.keySet();
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getFooter(String key)
    {
        if (footers != null)
        {
            return footers.get(key);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Collection<String> getFooterKeys()
    {
        if (footers != null)
        {
            return footers.keySet();
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean hasMultiplePayloads()
    {
        return (payloads != null);
    }

    @Override
    public byte[] getPayload()
    {
        if (payload != null)
        {
            return payload.getPayload();
        }
        return null;
    }

    @Override
    public Collection<byte[]> getPayloads()
    {
        Collection<byte[]> payloadCollection = null;
        if (payload != null)
        {
            payloadCollection = new ArrayList<byte[]>();
            payloadCollection.add(payload.getPayload());
        }
        if (payloads != null)
        {
            for (DoveMQPayload payload: payloads)
            {
                payloadCollection.add(payload.getPayload());
            }
        }
        return payloadCollection;
    }

    @Override
    public void setRoutingTag(String tag)
    {
        addApplicationProperty(ROUTING_TAG_KEY, tag);
    }

    @Override
    public void setTopicPublishHierarchy(String topicHierarchy)
    {
        addApplicationProperty(TOPIC_PUBLISH_HIERARCHY_KEY, topicHierarchy);
    }

    public DoveMQMessageImpl()
    {
        super();
        headerProperties = new HeaderPropertiesImpl();
        messageProperties = new MessagePropertiesImpl();
    }

    private DoveMQMessageImpl(HeaderPropertiesImpl headerProps, MessagePropertiesImpl messageProps)
    {
        super();
        headerProperties = headerProps;
        messageProperties = messageProps;
    }

    public void encode(CAMQPEncoder encoder)
    {
        headerProperties.encode(encoder);

        encoder.encodePropertiesMap(SYMBOL_DELIVERY_ANNOTATIONS, deliveryAnnotations);

        encoder.encodePropertiesMap(SYMBOL_MESSAGE_ANNOTATIONS, messageAnnotations);

        messageProperties.encode(encoder);

        encoder.encodePropertiesMap(SYMBOL_APPLICATION_ANNOTATIONS, applicationProperties);

        if (payload != null)
        {
            payload.encode(encoder);
        }

        if (payloads != null)
        {
            for (DoveMQPayload payload : payloads)
            {
                payload.encode(encoder);
            }
        }

        encoder.encodePropertiesMap(SYMBOL_FOOTERS, footers);
    }

    public static DoveMQMessageImpl decode(CAMQPSyncDecoder decoder)
    {
        HeaderPropertiesImpl headerProps = HeaderPropertiesImpl.decode(decoder);

        String symbolRead = decoder.readSymbol();
        assert(StringUtils.equals(symbolRead, SYMBOL_DELIVERY_ANNOTATIONS));
        Map<String, String> deliveryAnnotations = decoder.decodePropertiesMap();

        symbolRead = decoder.readSymbol();
        assert(StringUtils.equals(symbolRead, SYMBOL_MESSAGE_ANNOTATIONS));
        Map<String, String> messageAnnotations = decoder.decodePropertiesMap();

        MessagePropertiesImpl messageProps = MessagePropertiesImpl.decode(decoder);

        symbolRead = decoder.readSymbol();
        assert(StringUtils.equals(symbolRead, SYMBOL_APPLICATION_ANNOTATIONS));
        Map<String, String> applicationAnnotations = decoder.decodePropertiesMap();

        DoveMQMessageImpl message = new DoveMQMessageImpl(headerProps, messageProps);
        message.deliveryAnnotations = deliveryAnnotations;
        message.messageAnnotations = messageAnnotations;
        message.applicationProperties = applicationAnnotations;

        while (true)
        {
            symbolRead = decoder.readSymbol();
            if (symbolRead.equals(SYMBOL_FOOTERS))
            {
                break;
            }
            assert(StringUtils.equals(symbolRead, CAMQPProtocolConstants.SYMBOL_BINARY_PAYLOAD));
            byte[] payloadBytes = decoder.readBinaryPayload();
            if (message.payload == null)
            {
                message.payload = new DoveMQPayload(payloadBytes);
            }
            else
            {
                if (message.payloads == null)
                {
                    message.payloads = new ArrayList<DoveMQPayload>();
                }
                message.payloads.add(new DoveMQPayload(payloadBytes));
            }

        }
        message.footers = decoder.decodePropertiesMap();
        return message;
    }

    public CAMQPMessagePayload marshal()
    {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        encode(encoder);
        return new CAMQPMessagePayload(encoder.getEncodedBuffer());
    }

    public static DoveMQMessageImpl unmarshal(CAMQPMessagePayload payload)
    {
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(payload.getPayload());
        return DoveMQMessageImpl.decode(decoder);
    }

    public long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(long sourceHashCode)
    {
        this.sourceId = sourceHashCode;
    }

    private final HeaderPropertiesImpl headerProperties;
    private Map<String, String> deliveryAnnotations = null;
    private Map<String, String> messageAnnotations = null;
    private final MessagePropertiesImpl messageProperties;
    private Map<String, String> applicationProperties = null;
    private DoveMQPayload payload = null;
    private List<DoveMQPayload> payloads = null;
    private Map<String, String> footers = null;
    private long deliveryId = -1;
    private long sourceId = -1;
}
