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

public class CAMQPDefinitionRejected
{
    public static final String descriptor = "amqp:rejected:list";

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

    private boolean isSetError = false;
    public void setRequiredError(boolean val)
    {
        isSetError = val;
    }
    public boolean isSetError()
    {
        return isSetError;
    }
    private CAMQPDefinitionError error = null;
    public void setError(CAMQPDefinitionError val)
    {
        isSetError = true;
        error = val;
    }

    public CAMQPDefinitionError getError()
    {
        return error;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionRejected data)
    {
        long listSize = 2;
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
        if ((data.error != null) && (data.isSetError))
        {
            CAMQPDefinitionError.encode(encoder, data.error);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionRejected decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 2);
        CAMQPDefinitionRejected data = new CAMQPDefinitionRejected();

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

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            assert(controlName.equalsIgnoreCase(CAMQPDefinitionError.descriptor));
            data.error = CAMQPDefinitionError.decode(decoder);
            data.isSetError = true;
        }
        else
        {
            formatCode = decoder.readFormatCode();
            assert (formatCode == CAMQPFormatCodes.NULL);
        }
        return data;
    }
}