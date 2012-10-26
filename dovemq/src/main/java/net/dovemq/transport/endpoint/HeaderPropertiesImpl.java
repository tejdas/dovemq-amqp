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

import net.dovemq.api.HeaderProperties;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPDefinitionHeader;

import org.apache.commons.lang.StringUtils;

final class HeaderPropertiesImpl implements HeaderProperties
{
    @Override
    public boolean isDurable()
    {
        return properties.getDurable();
    }

    @Override
    public void setDurable(boolean durable)
    {
        properties.setDurable(durable);
    }

    @Override
    public int getPriority()
    {
        return properties.getPriority();
    }

    @Override
    public void setPriority(int val)
    {
        properties.setPriority(val);
    }

    @Override
    public long getTTL()
    {
        return properties.getTtl();
    }

    @Override
    public void setTTL(long ttl)
    {
        properties.setTtl(ttl);
    }

    @Override
    public long getDeliveryCount()
    {
        return properties.getDeliveryCount();
    }

    @Override
    public void setDeliveryCount(long deliveryCount)
    {
        properties.setDeliveryCount(deliveryCount);
    }

    HeaderPropertiesImpl(CAMQPDefinitionHeader properties)
    {
        super();
        this.properties = properties;
    }

    HeaderPropertiesImpl()
    {
        super();
        this.properties = new CAMQPDefinitionHeader();
    }

    void encode(CAMQPEncoder encoder)
    {
        CAMQPDefinitionHeader.encode(encoder, properties);
    }

    static HeaderPropertiesImpl decode(CAMQPSyncDecoder decoder)
    {
        String symbolRead = decoder.readSymbol();
        assert(StringUtils.equals(symbolRead, CAMQPDefinitionHeader.descriptor));
        CAMQPDefinitionHeader header = CAMQPDefinitionHeader.decode(decoder);
        return new HeaderPropertiesImpl(header);
    }

    private final CAMQPDefinitionHeader properties;
}
