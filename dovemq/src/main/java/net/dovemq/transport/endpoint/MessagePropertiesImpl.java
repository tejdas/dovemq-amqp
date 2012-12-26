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

import java.util.Date;

import net.dovemq.api.MessageProperties;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPDefinitionProperties;

import org.apache.commons.lang.StringUtils;

final class MessagePropertiesImpl implements MessageProperties {
    public MessagePropertiesImpl(CAMQPDefinitionProperties properties) {
        super();
        this.properties = properties;
    }

    public MessagePropertiesImpl() {
        super();
        this.properties = new CAMQPDefinitionProperties();
    }

    @Override
    public String getMessageId() {
        return (String) properties.getMessageId();
    }

    @Override
    public void setMessageId(String messageId) {
        properties.setMessageId(messageId);
    }

    @Override
    public String getCorrlelationId() {
        return (String) properties.getCorrelationId();
    }

    @Override
    public void setCorrlelationId(String corrlelationId) {
        properties.setCorrelationId(corrlelationId);
    }

    @Override
    public String getUserId() {
        return new String(properties.getUserId());
    }

    @Override
    public void setUserId(String userId) {
        properties.setUserId(userId.getBytes());
    }

    @Override
    public String getToAddress() {
        return (String) properties.getTo();
    }

    @Override
    public void setToAddress(String toAddress) {
        properties.setTo(toAddress);
    }

    @Override
    public String getReplyToAddress() {
        return (String) properties.getReplyTo();
    }

    @Override
    public void setReplyToAddress(String replyToAddress) {
        properties.setReplyTo(replyToAddress);
    }

    @Override
    public String getSubject() {
        return properties.getSubject();
    }

    @Override
    public void setSubject(String subject) {
        properties.setSubject(subject);
    }

    @Override
    public String getContentType() {
        return properties.getContentType();
    }

    @Override
    public void setContentType(String contentType) {
        properties.setContentType(contentType);
    }

    @Override
    public Date getCreationTime() {
        return properties.getCreationTime();
    }

    @Override
    public void setCreationTime(Date creationTime) {
        properties.setCreationTime(creationTime);
    }

    @Override
    public Date getExpiryTime() {
        return properties.getAbsoluteExpiryTime();
    }

    @Override
    public void setExpiryTime(Date expiryTime) {
        properties.setAbsoluteExpiryTime(expiryTime);
    }

    public void encode(CAMQPEncoder encoder) {
        CAMQPDefinitionProperties.encode(encoder, properties);
    }

    static MessagePropertiesImpl decode(CAMQPSyncDecoder decoder) {
        String symbolRead = decoder.readSymbol();
        assert (StringUtils.equals(symbolRead, CAMQPDefinitionProperties.descriptor));
        CAMQPDefinitionProperties properties = CAMQPDefinitionProperties.decode(decoder);
        return new MessagePropertiesImpl(properties);
    }

    private final CAMQPDefinitionProperties properties;
}
