package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.math.BigInteger;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionDeliveryState
{
    public static final String descriptor = "amqp:delivery-state:list";

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

    private boolean isSetSectionNumber = false;
    public void setRequiredSectionNumber(boolean val)
    {
        isSetSectionNumber = val;
    }
    public boolean isSetSectionNumber()
    {
        return isSetSectionNumber;
    }
    private Long sectionNumber = 0L;
    public void setSectionNumber(Long val)
    {
        isSetSectionNumber = true;
        sectionNumber = val;
    }

    public Long getSectionNumber()
    {
        return sectionNumber;
    }

    private boolean isSetSectionOffset = false;
    public void setRequiredSectionOffset(boolean val)
    {
        isSetSectionOffset = val;
    }
    public boolean isSetSectionOffset()
    {
        return isSetSectionOffset;
    }
    private java.math.BigInteger sectionOffset = null;
    public void setSectionOffset(java.math.BigInteger val)
    {
        isSetSectionOffset = true;
        sectionOffset = val;
    }

    public java.math.BigInteger getSectionOffset()
    {
        return sectionOffset;
    }

    private boolean isSetOutcome = false;
    public void setRequiredOutcome(boolean val)
    {
        isSetOutcome = val;
    }
    public boolean isSetOutcome()
    {
        return isSetOutcome;
    }
    private Object outcome = null;
    public void setOutcome(Object val)
    {
        isSetOutcome = true;
        outcome = val;
    }

    public Object getOutcome()
    {
        return outcome;
    }

    private boolean isSetTxnId = false;
    public void setRequiredTxnId(boolean val)
    {
        isSetTxnId = val;
    }
    public boolean isSetTxnId()
    {
        return isSetTxnId;
    }
    private String txnId = null;
    public void setTxnId(String val)
    {
        isSetTxnId = true;
        txnId = val;
    }

    public String getTxnId()
    {
        return txnId;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionDeliveryState data)
    {
        long listSize = 5;
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

        if (data.isSetSectionNumber)
        {
            encoder.writeUInt(data.sectionNumber);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetSectionOffset)
        {
            encoder.writeULong(data.sectionOffset);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.outcome != null) && (data.isSetOutcome))
        {
            if (data.outcome instanceof CAMQPDefinitionModified)
            {
                CAMQPDefinitionModified.encode(encoder, (CAMQPDefinitionModified) data.outcome);
            }
            else if (data.outcome instanceof CAMQPDefinitionReleased)
            {
                CAMQPDefinitionReleased.encode(encoder, (CAMQPDefinitionReleased) data.outcome);
            }
            else if (data.outcome instanceof CAMQPDefinitionAccepted)
            {
                CAMQPDefinitionAccepted.encode(encoder, (CAMQPDefinitionAccepted) data.outcome);
            }
            else if (data.outcome instanceof CAMQPDefinitionRejected)
            {
                CAMQPDefinitionRejected.encode(encoder, (CAMQPDefinitionRejected) data.outcome);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.txnId != null) && (data.isSetTxnId))
        {
            encoder.writeUTF8String(data.txnId);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionDeliveryState decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 5);
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();

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
            data.sectionNumber = decoder.readUInt();
            data.isSetSectionNumber = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.sectionOffset = decoder.readULong();
            data.isSetSectionOffset = true;
        }

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPDefinitionModified.descriptor))
            {
                data.outcome = CAMQPDefinitionModified.decode(decoder);
            }
            else if (controlName.equalsIgnoreCase(CAMQPDefinitionReleased.descriptor))
            {
                data.outcome = CAMQPDefinitionReleased.decode(decoder);
            }
            else if (controlName.equalsIgnoreCase(CAMQPDefinitionAccepted.descriptor))
            {
                data.outcome = CAMQPDefinitionAccepted.decode(decoder);
            }
            else if (controlName.equalsIgnoreCase(CAMQPDefinitionRejected.descriptor))
            {
                data.outcome = CAMQPDefinitionRejected.decode(decoder);
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
            data.txnId = decoder.readString(formatCode);
            data.isSetTxnId = true;
        }
        return data;
    }
}
