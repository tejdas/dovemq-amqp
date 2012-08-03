package net.dovemq.transport.protocol.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.math.BigInteger;
import net.dovemq.transport.protocol.*;

public class CAMQPControlAttach
{
    public static final String descriptor = "amqp:attach:list";

    private String name = null;
    public void setName(String val)
    {
        name = val;
    }

    public String getName()
    {
        return name;
    }

    private Long handle = 0L;
    public void setHandle(Long val)
    {
        handle = val;
    }

    public Long getHandle()
    {
        return handle;
    }

    private Boolean role = false;
    public void setRole(Boolean val)
    {
        role = val;
    }

    public Boolean getRole()
    {
        return role;
    }

    private boolean isSetSndSettleMode = false;
    public void setRequiredSndSettleMode(boolean val)
    {
        isSetSndSettleMode = val;
    }
    public boolean isSetSndSettleMode()
    {
        return isSetSndSettleMode;
    }
    private int sndSettleMode = 0;
    public void setSndSettleMode(int val)
    {
        isSetSndSettleMode = true;
        sndSettleMode = val;
    }

    public int getSndSettleMode()
    {
        return sndSettleMode;
    }

    private boolean isSetRcvSettleMode = false;
    public void setRequiredRcvSettleMode(boolean val)
    {
        isSetRcvSettleMode = val;
    }
    public boolean isSetRcvSettleMode()
    {
        return isSetRcvSettleMode;
    }
    private int rcvSettleMode = 0;
    public void setRcvSettleMode(int val)
    {
        isSetRcvSettleMode = true;
        rcvSettleMode = val;
    }

    public int getRcvSettleMode()
    {
        return rcvSettleMode;
    }

    private boolean isSetSource = false;
    public void setRequiredSource(boolean val)
    {
        isSetSource = val;
    }
    public boolean isSetSource()
    {
        return isSetSource;
    }
    private Object source = null;
    public void setSource(Object val)
    {
        isSetSource = true;
        source = val;
    }

    public Object getSource()
    {
        return source;
    }

    private boolean isSetTarget = false;
    public void setRequiredTarget(boolean val)
    {
        isSetTarget = val;
    }
    public boolean isSetTarget()
    {
        return isSetTarget;
    }
    private Object target = null;
    public void setTarget(Object val)
    {
        isSetTarget = true;
        target = val;
    }

    public Object getTarget()
    {
        return target;
    }

    private boolean isSetUnsettled = false;
    public void setRequiredUnsettled(boolean val)
    {
        isSetUnsettled = val;
    }
    public boolean isSetUnsettled()
    {
        return isSetUnsettled;
    }
    private final Map<String, String> unsettled = new HashMap<String, String>();
    public Map<String, String> getUnsettled()
    {
        return unsettled;
    }

    private boolean isSetInitialDeliveryCount = false;
    public void setRequiredInitialDeliveryCount(boolean val)
    {
        isSetInitialDeliveryCount = val;
    }
    public boolean isSetInitialDeliveryCount()
    {
        return isSetInitialDeliveryCount;
    }
    private Long initialDeliveryCount = 0L;
    public void setInitialDeliveryCount(Long val)
    {
        isSetInitialDeliveryCount = true;
        initialDeliveryCount = val;
    }

    public Long getInitialDeliveryCount()
    {
        return initialDeliveryCount;
    }

    private boolean isSetMaxMessageSize = false;
    public void setRequiredMaxMessageSize(boolean val)
    {
        isSetMaxMessageSize = val;
    }
    public boolean isSetMaxMessageSize()
    {
        return isSetMaxMessageSize;
    }
    private java.math.BigInteger maxMessageSize = null;
    public void setMaxMessageSize(java.math.BigInteger val)
    {
        isSetMaxMessageSize = true;
        maxMessageSize = val;
    }

    public java.math.BigInteger getMaxMessageSize()
    {
        return maxMessageSize;
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

    public static void encode(CAMQPEncoder encoder, CAMQPControlAttach data)
    {
        long listSize = 13;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.name != null)
        {
            encoder.writeUTF8String(data.name);
        }
        else
        {
            encoder.writeNull();
        }

        encoder.writeUInt(data.handle);

        encoder.writeBoolean(data.role);

        if (data.isSetSndSettleMode)
        {
            encoder.writeUByte(data.sndSettleMode);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetRcvSettleMode)
        {
            encoder.writeUByte(data.rcvSettleMode);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.source != null) && (data.isSetSource))
        {
            if (data.source instanceof CAMQPDefinitionSource)
            {
                CAMQPDefinitionSource.encode(encoder, (CAMQPDefinitionSource) data.source);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.target != null) && (data.isSetTarget))
        {
            if (data.target instanceof CAMQPDefinitionTarget)
            {
                CAMQPDefinitionTarget.encode(encoder, (CAMQPDefinitionTarget) data.target);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.unsettled != null) && (data.unsettled.size() > 0) && (data.isSetUnsettled))
        {
            int size = data.unsettled.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.unsettled.entrySet();
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

        if (data.isSetInitialDeliveryCount)
        {
            encoder.writeUInt(data.initialDeliveryCount);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetMaxMessageSize)
        {
            encoder.writeULong(data.maxMessageSize);
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
    public static CAMQPControlAttach decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 13);
        CAMQPControlAttach data = new CAMQPControlAttach();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.name = decoder.readString(formatCode);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.handle = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.role = (formatCode == CAMQPFormatCodes.TRUE);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.sndSettleMode = decoder.readUByte();
            data.isSetSndSettleMode = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.rcvSettleMode = decoder.readUByte();
            data.isSetRcvSettleMode = true;
        }

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPDefinitionSource.descriptor))
            {
                data.source = CAMQPDefinitionSource.decode(decoder);
            }
        }
        else
        {
            formatCode = decoder.readFormatCode();
            assert (formatCode == CAMQPFormatCodes.NULL);
        }

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPDefinitionTarget.descriptor))
            {
                data.target = CAMQPDefinitionTarget.decode(decoder);
            }
        }
        else
        {
            formatCode = decoder.readFormatCode();
            assert (formatCode == CAMQPFormatCodes.NULL);
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
                    data.unsettled.put(innerKey, innerVal);
                }
            }
            data.isSetUnsettled = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.initialDeliveryCount = decoder.readUInt();
            data.isSetInitialDeliveryCount = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.maxMessageSize = decoder.readULong();
            data.isSetMaxMessageSize = true;
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
