package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionError
{
    public static final String descriptor = "amqp:error:list";

    private String condition = null;
    public void setCondition(String val)
    {
        condition = val;
    }

    public String getCondition()
    {
        return condition;
    }

    private boolean isSetDescription = false;
    public void setRequiredDescription(boolean val)
    {
        isSetDescription = val;
    }
    public boolean isSetDescription()
    {
        return isSetDescription;
    }
    private String description = null;
    public void setDescription(String val)
    {
        isSetDescription = true;
        description = val;
    }

    public String getDescription()
    {
        return description;
    }

    private boolean isSetInfo = false;
    public void setRequiredInfo(boolean val)
    {
        isSetInfo = val;
    }
    public boolean isSetInfo()
    {
        return isSetInfo;
    }
    private final Map<String, String> info = new HashMap<String, String>();
    public Map<String, String> getInfo()
    {
        return info;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionError data)
    {
        long listSize = 3;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.condition != null)
        {
            encoder.writeSymbol(data.condition);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.description != null) && (data.isSetDescription))
        {
            encoder.writeUTF8String(data.description);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.info != null) && (data.info.size() > 0) && (data.isSetInfo))
        {
            int size = data.info.size();
            encoder.writeMapHeader(size);
            Set<Entry<String, String>> entries = data.info.entrySet();
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
    public static CAMQPDefinitionError decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 3);
        CAMQPDefinitionError data = new CAMQPDefinitionError();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.condition = decoder.readString(formatCode);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.description = decoder.readString(formatCode);
            data.isSetDescription = true;
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
                    data.info.put(innerKey, innerVal);
                }
            }
            data.isSetInfo = true;
        }
        return data;
    }
}
