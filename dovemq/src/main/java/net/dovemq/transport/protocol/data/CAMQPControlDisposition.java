package net.dovemq.transport.protocol.data;

import net.dovemq.transport.protocol.*;

public class CAMQPControlDisposition
{
    public static final String descriptor = "amqp:disposition:list";

    private Boolean role = false;
    public void setRole(Boolean val)
    {
        role = val;
    }

    public Boolean getRole()
    {
        return role;
    }

    private boolean isSetBatchable = false;
    public void setRequiredBatchable(boolean val)
    {
        isSetBatchable = val;
    }
    public boolean isSetBatchable()
    {
        return isSetBatchable;
    }
    private Boolean batchable = false;
    public void setBatchable(Boolean val)
    {
        isSetBatchable = true;
        batchable = val;
    }

    public Boolean getBatchable()
    {
        return batchable;
    }

    private Long first = 0L;
    public void setFirst(Long val)
    {
        first = val;
    }

    public Long getFirst()
    {
        return first;
    }

    private boolean isSetLast = false;
    public void setRequiredLast(boolean val)
    {
        isSetLast = val;
    }
    public boolean isSetLast()
    {
        return isSetLast;
    }
    private Long last = 0L;
    public void setLast(Long val)
    {
        isSetLast = true;
        last = val;
    }

    public Long getLast()
    {
        return last;
    }

    private boolean isSetSettled = false;
    public void setRequiredSettled(boolean val)
    {
        isSetSettled = val;
    }
    public boolean isSetSettled()
    {
        return isSetSettled;
    }
    private Boolean settled = false;
    public void setSettled(Boolean val)
    {
        isSetSettled = true;
        settled = val;
    }

    public Boolean getSettled()
    {
        return settled;
    }

    private boolean isSetState = false;
    public void setRequiredState(boolean val)
    {
        isSetState = val;
    }
    public boolean isSetState()
    {
        return isSetState;
    }
    private Object state = null;
    public void setState(Object val)
    {
        isSetState = true;
        state = val;
    }

    public Object getState()
    {
        return state;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPControlDisposition data)
    {
        long listSize = 6;
        encoder.writeListDescriptor(descriptor, listSize);

        encoder.writeBoolean(data.role);

        if (data.isSetBatchable)
        {
            encoder.writeBoolean(data.batchable);
        }
        else
        {
            encoder.writeNull();
        }

        encoder.writeUInt(data.first);

        if (data.isSetLast)
        {
            encoder.writeUInt(data.last);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetSettled)
        {
            encoder.writeBoolean(data.settled);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.state != null) && (data.isSetState))
        {
            if (data.state instanceof CAMQPDefinitionDeliveryState)
            {
                CAMQPDefinitionDeliveryState.encode(encoder, (CAMQPDefinitionDeliveryState) data.state);
            }
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPControlDisposition decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 6);
        CAMQPControlDisposition data = new CAMQPControlDisposition();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.role = (formatCode == CAMQPFormatCodes.TRUE);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.batchable = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetBatchable = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.first = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.last = decoder.readUInt();
            data.isSetLast = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.settled = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetSettled = true;
        }

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor))
            {
                data.state = CAMQPDefinitionDeliveryState.decode(decoder);
            }
        }
        else
        {
            formatCode = decoder.readFormatCode();
            assert (formatCode == CAMQPFormatCodes.NULL);
        }
        return data;
    }
}
