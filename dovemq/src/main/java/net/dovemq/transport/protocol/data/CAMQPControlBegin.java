package net.dovemq.transport.protocol.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPControlBegin
{
    public static final String descriptor = "amqp:begin:list";

    private boolean isSetRemoteChannel = false;
    public void setRequiredRemoteChannel(boolean val)
    {
        isSetRemoteChannel = val;
    }
    public boolean isSetRemoteChannel()
    {
        return isSetRemoteChannel;
    }
    private Integer remoteChannel = 0;
    public void setRemoteChannel(Integer val)
    {
        isSetRemoteChannel = true;
        remoteChannel = val;
    }

    public Integer getRemoteChannel()
    {
        return remoteChannel;
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

    private Long incomingWindow = 0L;
    public void setIncomingWindow(Long val)
    {
        incomingWindow = val;
    }

    public Long getIncomingWindow()
    {
        return incomingWindow;
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

    private boolean isSetHandleMax = false;
    public void setRequiredHandleMax(boolean val)
    {
        isSetHandleMax = val;
    }
    public boolean isSetHandleMax()
    {
        return isSetHandleMax;
    }
    private Long handleMax = 0L;
    public void setHandleMax(Long val)
    {
        isSetHandleMax = true;
        handleMax = val;
    }

    public Long getHandleMax()
    {
        return handleMax;
    }

    private boolean isSetOfferedCapabilities = false;
    public void setRequiredOfferedCapabilities(boolean val)
    {
        isSetOfferedCapabilities = val;
    }
    public boolean isSetOfferedCapabilities()
    {
        return isSetOfferedCapabilities;
    }
    private Collection<String> offeredCapabilities = new ArrayList<String>();
    public void addOfferedCapabilities(String val)
    {
        isSetOfferedCapabilities = true;
        offeredCapabilities.add(val);
    }

    public Collection<String> getOfferedCapabilities()
    {
        return offeredCapabilities;
    }

    private boolean isSetDesiredCapabilities = false;
    public void setRequiredDesiredCapabilities(boolean val)
    {
        isSetDesiredCapabilities = val;
    }
    public boolean isSetDesiredCapabilities()
    {
        return isSetDesiredCapabilities;
    }
    private Collection<String> desiredCapabilities = new ArrayList<String>();
    public void addDesiredCapabilities(String val)
    {
        isSetDesiredCapabilities = true;
        desiredCapabilities.add(val);
    }

    public Collection<String> getDesiredCapabilities()
    {
        return desiredCapabilities;
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

    public static void encode(CAMQPEncoder encoder, CAMQPControlBegin data)
    {
        long listSize = 8;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.isSetRemoteChannel)
        {
            encoder.writeUShort(data.remoteChannel);
        }
        else
        {
            encoder.writeNull();
        }

        encoder.writeUInt(data.nextOutgoingId);

        encoder.writeUInt(data.incomingWindow);

        encoder.writeUInt(data.outgoingWindow);

        if (data.isSetHandleMax)
        {
            encoder.writeUInt(data.handleMax);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.offeredCapabilities != null) && (data.isSetOfferedCapabilities))
        {
            long offeredCapabilitiesSize = data.offeredCapabilities.size();
            if (offeredCapabilitiesSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(offeredCapabilitiesSize, CAMQPFormatCodes.SYM8);
                for (String val : data.offeredCapabilities)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(offeredCapabilitiesSize);
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

        if ((data.desiredCapabilities != null) && (data.isSetDesiredCapabilities))
        {
            long desiredCapabilitiesSize = data.desiredCapabilities.size();
            if (desiredCapabilitiesSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(desiredCapabilitiesSize, CAMQPFormatCodes.SYM8);
                for (String val : data.desiredCapabilities)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(desiredCapabilitiesSize);
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
    public static CAMQPControlBegin decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 8);
        CAMQPControlBegin data = new CAMQPControlBegin();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.remoteChannel = decoder.readUShort();
            data.isSetRemoteChannel = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.nextOutgoingId = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.incomingWindow = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.outgoingWindow = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.handleMax = decoder.readUInt();
            data.isSetHandleMax = true;
        }

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.offeredCapabilities.add(decoder.readString(compoundHeader.elementFormatCode));
            }
        }

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.desiredCapabilities.add(decoder.readString(compoundHeader.elementFormatCode));
            }
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
