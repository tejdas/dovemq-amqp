package net.dovemq.transport.protocol.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPControlOpen
{
    public static final String descriptor = "amqp:open:list";

    private String containerId = null;
    public void setContainerId(String val)
    {
        containerId = val;
    }

    public String getContainerId()
    {
        return containerId;
    }

    private boolean isSetHostname = false;
    public void setRequiredHostname(boolean val)
    {
        isSetHostname = val;
    }
    public boolean isSetHostname()
    {
        return isSetHostname;
    }
    private String hostname = null;
    public void setHostname(String val)
    {
        isSetHostname = true;
        hostname = val;
    }

    public String getHostname()
    {
        return hostname;
    }

    private boolean isSetMaxFrameSize = false;
    public void setRequiredMaxFrameSize(boolean val)
    {
        isSetMaxFrameSize = val;
    }
    public boolean isSetMaxFrameSize()
    {
        return isSetMaxFrameSize;
    }
    private Long maxFrameSize = 4294967295L;
    public void setMaxFrameSize(Long val)
    {
        isSetMaxFrameSize = true;
        maxFrameSize = val;
    }

    public Long getMaxFrameSize()
    {
        return maxFrameSize;
    }

    private boolean isSetChannelMax = false;
    public void setRequiredChannelMax(boolean val)
    {
        isSetChannelMax = val;
    }
    public boolean isSetChannelMax()
    {
        return isSetChannelMax;
    }
    private Integer channelMax = 65535;
    public void setChannelMax(Integer val)
    {
        isSetChannelMax = true;
        channelMax = val;
    }

    public Integer getChannelMax()
    {
        return channelMax;
    }

    private boolean isSetIdleTimeOut = false;
    public void setRequiredIdleTimeOut(boolean val)
    {
        isSetIdleTimeOut = val;
    }
    public boolean isSetIdleTimeOut()
    {
        return isSetIdleTimeOut;
    }
    private Long idleTimeOut = 0L;
    public void setIdleTimeOut(Long val)
    {
        isSetIdleTimeOut = true;
        idleTimeOut = val;
    }

    public Long getIdleTimeOut()
    {
        return idleTimeOut;
    }

    private boolean isSetOutgoingLocales = false;
    public void setRequiredOutgoingLocales(boolean val)
    {
        isSetOutgoingLocales = val;
    }
    public boolean isSetOutgoingLocales()
    {
        return isSetOutgoingLocales;
    }
    private Collection<String> outgoingLocales = new ArrayList<String>();
    public void addOutgoingLocales(String val)
    {
        isSetOutgoingLocales = true;
        outgoingLocales.add(val);
    }

    public Collection<String> getOutgoingLocales()
    {
        return outgoingLocales;
    }

    private boolean isSetIncomingLocales = false;
    public void setRequiredIncomingLocales(boolean val)
    {
        isSetIncomingLocales = val;
    }
    public boolean isSetIncomingLocales()
    {
        return isSetIncomingLocales;
    }
    private Collection<String> incomingLocales = new ArrayList<String>();
    public void addIncomingLocales(String val)
    {
        isSetIncomingLocales = true;
        incomingLocales.add(val);
    }

    public Collection<String> getIncomingLocales()
    {
        return incomingLocales;
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

    public static void encode(CAMQPEncoder encoder, CAMQPControlOpen data)
    {
        long listSize = 10;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.containerId != null)
        {
            encoder.writeUTF8String(data.containerId);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.hostname != null) && (data.isSetHostname))
        {
            encoder.writeUTF8String(data.hostname);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetMaxFrameSize)
        {
            encoder.writeUInt(data.maxFrameSize);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetChannelMax)
        {
            encoder.writeUShort(data.channelMax);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetIdleTimeOut)
        {
            encoder.writeUInt(data.idleTimeOut);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.outgoingLocales != null) && (data.isSetOutgoingLocales))
        {
            long outgoingLocalesSize = data.outgoingLocales.size();
            if (outgoingLocalesSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(outgoingLocalesSize, CAMQPFormatCodes.SYM8);
                for (String val : data.outgoingLocales)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(outgoingLocalesSize);
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

        if ((data.incomingLocales != null) && (data.isSetIncomingLocales))
        {
            long incomingLocalesSize = data.incomingLocales.size();
            if (incomingLocalesSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(incomingLocalesSize, CAMQPFormatCodes.SYM8);
                for (String val : data.incomingLocales)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(incomingLocalesSize);
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
    public static CAMQPControlOpen decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 10);
        CAMQPControlOpen data = new CAMQPControlOpen();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.containerId = decoder.readString(formatCode);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.hostname = decoder.readString(formatCode);
            data.isSetHostname = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.maxFrameSize = decoder.readUInt();
            data.isSetMaxFrameSize = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.channelMax = decoder.readUShort();
            data.isSetChannelMax = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.idleTimeOut = decoder.readUInt();
            data.isSetIdleTimeOut = true;
        }

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.outgoingLocales.add(decoder.readString(compoundHeader.elementFormatCode));
            }
        }

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.incomingLocales.add(decoder.readString(compoundHeader.elementFormatCode));
            }
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
