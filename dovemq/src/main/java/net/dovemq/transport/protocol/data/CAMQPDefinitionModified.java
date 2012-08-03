package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionModified
{
    public static final String descriptor = "amqp:modified:list";

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

    private boolean isSetDeliveryFailed = false;
    public void setRequiredDeliveryFailed(boolean val)
    {
        isSetDeliveryFailed = val;
    }
    public boolean isSetDeliveryFailed()
    {
        return isSetDeliveryFailed;
    }
    private Boolean deliveryFailed = false;
    public void setDeliveryFailed(Boolean val)
    {
        isSetDeliveryFailed = true;
        deliveryFailed = val;
    }

    public Boolean getDeliveryFailed()
    {
        return deliveryFailed;
    }

    private boolean isSetUndeliverableHere = false;
    public void setRequiredUndeliverableHere(boolean val)
    {
        isSetUndeliverableHere = val;
    }
    public boolean isSetUndeliverableHere()
    {
        return isSetUndeliverableHere;
    }
    private Boolean undeliverableHere = false;
    public void setUndeliverableHere(Boolean val)
    {
        isSetUndeliverableHere = true;
        undeliverableHere = val;
    }

    public Boolean getUndeliverableHere()
    {
        return undeliverableHere;
    }

    private boolean isSetMessageAttrs = false;
    public void setRequiredMessageAttrs(boolean val)
    {
        isSetMessageAttrs = val;
    }
    public boolean isSetMessageAttrs()
    {
        return isSetMessageAttrs;
    }
    private final Map<String, String> messageAttrs = new HashMap<String, String>();
    public Map<String, String> getMessageAttrs()
    {
        return messageAttrs;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionModified data)
    {
        long listSize = 4;
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

        if (data.isSetDeliveryFailed)
        {
            encoder.writeBoolean(data.deliveryFailed);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetUndeliverableHere)
        {
            encoder.writeBoolean(data.undeliverableHere);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.messageAttrs != null) && (data.messageAttrs.size() > 0) && (data.isSetMessageAttrs))
        {
            int size = data.messageAttrs.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.messageAttrs.entrySet();
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
    public static CAMQPDefinitionModified decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 4);
        CAMQPDefinitionModified data = new CAMQPDefinitionModified();

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
            data.deliveryFailed = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetDeliveryFailed = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.undeliverableHere = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetUndeliverableHere = true;
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
                    data.messageAttrs.put(innerKey, innerVal);
                }
            }
            data.isSetMessageAttrs = true;
        }
        return data;
    }
}
