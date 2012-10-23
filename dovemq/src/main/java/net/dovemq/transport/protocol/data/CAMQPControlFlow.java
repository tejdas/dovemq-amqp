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

/**
 * This file was auto-generated by dovemq gentools.
 * Do not modify.
 */
package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPControlFlow
{
    public static final String descriptor = "amqp:flow:list";

    private boolean isSetNextIncomingId = false;
    public void setRequiredNextIncomingId(boolean val)
    {
        isSetNextIncomingId = val;
    }
    public boolean isSetNextIncomingId()
    {
        return isSetNextIncomingId;
    }
    private Long nextIncomingId = 0L;
    public void setNextIncomingId(Long val)
    {
        isSetNextIncomingId = true;
        nextIncomingId = val;
    }

    public Long getNextIncomingId()
    {
        return nextIncomingId;
    }

    private Long incomingWindow = 0L;
    public void setIncomingWindow(Long val)
    {
        incomingWindow = val;
    }

    public Long getIncomingWindow()
    {
        return incomingWindow;
    }

    private Long nextOutgoingId = 0L;
    public void setNextOutgoingId(Long val)
    {
        nextOutgoingId = val;
    }

    public Long getNextOutgoingId()
    {
        return nextOutgoingId;
    }

    private Long outgoingWindow = 0L;
    public void setOutgoingWindow(Long val)
    {
        outgoingWindow = val;
    }

    public Long getOutgoingWindow()
    {
        return outgoingWindow;
    }

    private boolean isSetHandle = false;
    public void setRequiredHandle(boolean val)
    {
        isSetHandle = val;
    }
    public boolean isSetHandle()
    {
        return isSetHandle;
    }
    private Long handle = 0L;
    public void setHandle(Long val)
    {
        isSetHandle = true;
        handle = val;
    }

    public Long getHandle()
    {
        return handle;
    }

    private boolean isSetDeliveryCount = false;
    public void setRequiredDeliveryCount(boolean val)
    {
        isSetDeliveryCount = val;
    }
    public boolean isSetDeliveryCount()
    {
        return isSetDeliveryCount;
    }
    private Long deliveryCount = 0L;
    public void setDeliveryCount(Long val)
    {
        isSetDeliveryCount = true;
        deliveryCount = val;
    }

    public Long getDeliveryCount()
    {
        return deliveryCount;
    }

    private boolean isSetLinkCredit = false;
    public void setRequiredLinkCredit(boolean val)
    {
        isSetLinkCredit = val;
    }
    public boolean isSetLinkCredit()
    {
        return isSetLinkCredit;
    }
    private Long linkCredit = 0L;
    public void setLinkCredit(Long val)
    {
        isSetLinkCredit = true;
        linkCredit = val;
    }

    public Long getLinkCredit()
    {
        return linkCredit;
    }

    private boolean isSetAvailable = false;
    public void setRequiredAvailable(boolean val)
    {
        isSetAvailable = val;
    }
    public boolean isSetAvailable()
    {
        return isSetAvailable;
    }
    private Long available = 0L;
    public void setAvailable(Long val)
    {
        isSetAvailable = true;
        available = val;
    }

    public Long getAvailable()
    {
        return available;
    }

    private boolean isSetDrain = false;
    public void setRequiredDrain(boolean val)
    {
        isSetDrain = val;
    }
    public boolean isSetDrain()
    {
        return isSetDrain;
    }
    private Boolean drain = false;
    public void setDrain(Boolean val)
    {
        isSetDrain = true;
        drain = val;
    }

    public Boolean getDrain()
    {
        return drain;
    }

    private boolean isSetEcho = false;
    public void setRequiredEcho(boolean val)
    {
        isSetEcho = val;
    }
    public boolean isSetEcho()
    {
        return isSetEcho;
    }
    private Boolean echo = false;
    public void setEcho(Boolean val)
    {
        isSetEcho = true;
        echo = val;
    }

    public Boolean getEcho()
    {
        return echo;
    }

    private boolean isSetProperties = false;
    public void setRequiredProperties(boolean val)
    {
        isSetProperties = val;
    }
    public boolean isSetProperties()
    {
        return isSetProperties;
    }
    private final Map<String, String> properties = new HashMap<String, String>();
    public Map<String, String> getProperties()
    {
        return properties;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPControlFlow data)
    {
        long listSize = 11;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.isSetNextIncomingId)
        {
            encoder.writeUInt(data.nextIncomingId);
        }
        else
        {
            encoder.writeNull();
        }

        encoder.writeUInt(data.incomingWindow);

        encoder.writeUInt(data.nextOutgoingId);

        encoder.writeUInt(data.outgoingWindow);

        if (data.isSetHandle)
        {
            encoder.writeUInt(data.handle);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetDeliveryCount)
        {
            encoder.writeUInt(data.deliveryCount);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetLinkCredit)
        {
            encoder.writeUInt(data.linkCredit);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetAvailable)
        {
            encoder.writeUInt(data.available);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetDrain)
        {
            encoder.writeBoolean(data.drain);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetEcho)
        {
            encoder.writeBoolean(data.echo);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.properties != null) && (data.properties.size() > 0) && (data.isSetProperties))
        {
            int size = data.properties.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.properties.entrySet();
            for (Entry<String, String> entry : entries)
            {
                encoder.writeSymbol(entry.getKey());
                encoder.writeUTF8String(entry.getValue());
            }
            encoder.fillCompoundSize(size);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPControlFlow decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 11);
        CAMQPControlFlow data = new CAMQPControlFlow();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.nextIncomingId = decoder.readUInt();
            data.isSetNextIncomingId = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.incomingWindow = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.nextOutgoingId = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.outgoingWindow = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.handle = decoder.readUInt();
            data.isSetHandle = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.deliveryCount = decoder.readUInt();
            data.isSetDeliveryCount = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.linkCredit = decoder.readUInt();
            data.isSetLinkCredit = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.available = decoder.readUInt();
            data.isSetAvailable = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.drain = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetDrain = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.echo = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetEcho = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            assert((formatCode == CAMQPFormatCodes.MAP8) || (formatCode == CAMQPFormatCodes.MAP32));
            long innerMapSize = decoder.readMapCount(formatCode);
            for (long innerIndex = 0; innerIndex < innerMapSize; innerIndex++)
            {
                String innerKey = null;
                formatCode = decoder.readFormatCode();
                if (formatCode == CAMQPFormatCodes.SYM8)
                {
                    innerKey = decoder.readString(formatCode);
                }
                String innerVal = null;
                formatCode = decoder.readFormatCode();
                if (formatCode != CAMQPFormatCodes.NULL)
                {
                    innerVal = decoder.readString(formatCode);
                }
                if ((innerKey != null) && (innerVal != null))
                {
                    data.properties.put(innerKey, innerVal);
                }
            }
            data.isSetProperties = true;
        }
        return data;
    }
}
