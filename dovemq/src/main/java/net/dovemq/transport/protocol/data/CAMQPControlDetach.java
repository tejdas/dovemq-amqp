package net.dovemq.transport.protocol.data;

import net.dovemq.transport.protocol.*;

public class CAMQPControlDetach
{
    public static final String descriptor = "amqp:detach:list";

    private Long handle = 0L;
    public void setHandle(Long val)
    {
        handle = val;
    }

    public Long getHandle()
    {
        return handle;
    }

    private boolean isSetClosed = false;
    public void setRequiredClosed(boolean val)
    {
        isSetClosed = val;
    }
    public boolean isSetClosed()
    {
        return isSetClosed;
    }
    private Boolean closed = false;
    public void setClosed(Boolean val)
    {
        isSetClosed = true;
        closed = val;
    }

    public Boolean getClosed()
    {
        return closed;
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

    public static void encode(CAMQPEncoder encoder, CAMQPControlDetach data)
    {
        long listSize = 3;
        encoder.writeListDescriptor(descriptor, listSize);

        encoder.writeUInt(data.handle);

        if (data.isSetClosed)
        {
            encoder.writeBoolean(data.closed);
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
    public static CAMQPControlDetach decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 3);
        CAMQPControlDetach data = new CAMQPControlDetach();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.handle = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.closed = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetClosed = true;
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
