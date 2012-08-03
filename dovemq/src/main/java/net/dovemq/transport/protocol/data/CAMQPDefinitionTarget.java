package net.dovemq.transport.protocol.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionTarget
{
    public static final String descriptor = "amqp:target:list";

    private boolean isSetOptions = false;
    public void setRequiredOptions(boolean val)
    {
        isSetOptions = val;
    }
    public boolean isSetOptions()
    {
        return isSetOptions;
    }
    private final Map<String, String> options = new HashMap<String, String>();
    public Map<String, String> getOptions()
    {
        return options;
    }

    private boolean isSetAddress = false;
    public void setRequiredAddress(boolean val)
    {
        isSetAddress = val;
    }
    public boolean isSetAddress()
    {
        return isSetAddress;
    }
    private Object address = null;
    public void setAddress(Object val)
    {
        isSetAddress = true;
        address = val;
    }

    public Object getAddress()
    {
        return address;
    }

    private boolean isSetDurable = false;
    public void setRequiredDurable(boolean val)
    {
        isSetDurable = val;
    }
    public boolean isSetDurable()
    {
        return isSetDurable;
    }
    private Long durable = 0L;
    public void setDurable(Long val)
    {
        isSetDurable = true;
        durable = val;
    }

    public Long getDurable()
    {
        return durable;
    }

    private boolean isSetExpiryPolicy = false;
    public void setRequiredExpiryPolicy(boolean val)
    {
        isSetExpiryPolicy = val;
    }
    public boolean isSetExpiryPolicy()
    {
        return isSetExpiryPolicy;
    }
    private String expiryPolicy = null;
    public void setExpiryPolicy(String val)
    {
        isSetExpiryPolicy = true;
        expiryPolicy = val;
    }

    public String getExpiryPolicy()
    {
        return expiryPolicy;
    }

    private boolean isSetTimeout = false;
    public void setRequiredTimeout(boolean val)
    {
        isSetTimeout = val;
    }
    public boolean isSetTimeout()
    {
        return isSetTimeout;
    }
    private Long timeout = 0L;
    public void setTimeout(Long val)
    {
        isSetTimeout = true;
        timeout = val;
    }

    public Long getTimeout()
    {
        return timeout;
    }

    private boolean isSetDynamic = false;
    public void setRequiredDynamic(boolean val)
    {
        isSetDynamic = val;
    }
    public boolean isSetDynamic()
    {
        return isSetDynamic;
    }
    private Boolean dynamic = false;
    public void setDynamic(Boolean val)
    {
        isSetDynamic = true;
        dynamic = val;
    }

    public Boolean getDynamic()
    {
        return dynamic;
    }

    private boolean isSetDynamicNodeProperties = false;
    public void setRequiredDynamicNodeProperties(boolean val)
    {
        isSetDynamicNodeProperties = val;
    }
    public boolean isSetDynamicNodeProperties()
    {
        return isSetDynamicNodeProperties;
    }
    private final Map<String, String> dynamicNodeProperties = new HashMap<String, String>();
    public Map<String, String> getDynamicNodeProperties()
    {
        return dynamicNodeProperties;
    }

    private boolean isSetCapabilities = false;
    public void setRequiredCapabilities(boolean val)
    {
        isSetCapabilities = val;
    }
    public boolean isSetCapabilities()
    {
        return isSetCapabilities;
    }
    private Collection<String> capabilities = new ArrayList<String>();
    public void addCapabilities(String val)
    {
        isSetCapabilities = true;
        capabilities.add(val);
    }

    public Collection<String> getCapabilities()
    {
        return capabilities;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionTarget data)
    {
        long listSize = 8;
        encoder.writeListDescriptor(descriptor, listSize);

        if ((data.options != null) && (data.options.size() > 0) && (data.isSetOptions))
        {
            int size = data.options.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.options.entrySet();
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

        if ((data.address != null) && (data.isSetAddress))
        {
            if (data.address instanceof String)
            {
                encoder.writeUTF8String((String) data.address);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetDurable)
        {
            encoder.writeUInt(data.durable);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.expiryPolicy != null) && (data.isSetExpiryPolicy))
        {
            encoder.writeSymbol(data.expiryPolicy);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetTimeout)
        {
            encoder.writeUInt(data.timeout);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetDynamic)
        {
            encoder.writeBoolean(data.dynamic);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.dynamicNodeProperties != null) && (data.dynamicNodeProperties.size() > 0) && (data.isSetDynamicNodeProperties))
        {
            int size = data.dynamicNodeProperties.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.dynamicNodeProperties.entrySet();
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

        if ((data.capabilities != null) && (data.isSetCapabilities))
        {
            long capabilitiesSize = data.capabilities.size();
            if (capabilitiesSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(capabilitiesSize, CAMQPFormatCodes.SYM8);
                for (String val : data.capabilities)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(capabilitiesSize);
            }
            else
            {
                encoder.writeNull();
            }
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionTarget decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 8);
        CAMQPDefinitionTarget data = new CAMQPDefinitionTarget();

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
                    data.options.put(innerKey, innerVal);
                }
            }
            data.isSetOptions = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            if (formatCode == CAMQPFormatCodes.STR8_UTF8)
            {
                data.address = decoder.readString(formatCode);
            }
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.durable = decoder.readUInt();
            data.isSetDurable = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.expiryPolicy = decoder.readString(formatCode);
            data.isSetExpiryPolicy = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.timeout = decoder.readUInt();
            data.isSetTimeout = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.dynamic = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetDynamic = true;
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
                    data.dynamicNodeProperties.put(innerKey, innerVal);
                }
            }
            data.isSetDynamicNodeProperties = true;
        }

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.capabilities.add(decoder.readString(compoundHeader.elementFormatCode));
            }
        }
        return data;
    }
}
