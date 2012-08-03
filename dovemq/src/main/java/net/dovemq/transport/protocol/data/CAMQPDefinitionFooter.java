package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionFooter
{
    public static final String descriptor = "amqp:footer:list";

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

    private boolean isSetDeliveryAttrs = false;
    public void setRequiredDeliveryAttrs(boolean val)
    {
        isSetDeliveryAttrs = val;
    }
    public boolean isSetDeliveryAttrs()
    {
        return isSetDeliveryAttrs;
    }
    private final Map<String, String> deliveryAttrs = new HashMap<String, String>();
    public Map<String, String> getDeliveryAttrs()
    {
        return deliveryAttrs;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionFooter data)
    {
        long listSize = 2;
        encoder.writeListDescriptor(descriptor, listSize);

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

        if ((data.deliveryAttrs != null) && (data.deliveryAttrs.size() > 0) && (data.isSetDeliveryAttrs))
        {
            int size = data.deliveryAttrs.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.deliveryAttrs.entrySet();
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
    public static CAMQPDefinitionFooter decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 2);
        CAMQPDefinitionFooter data = new CAMQPDefinitionFooter();

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
                    data.deliveryAttrs.put(innerKey, innerVal);
                }
            }
            data.isSetDeliveryAttrs = true;
        }
        return data;
    }
}
