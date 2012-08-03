package net.dovemq.transport.protocol.data;

import net.dovemq.transport.protocol.*;

public class CAMQPControlEnd
{
    public static final String descriptor = "amqp:end:list";

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

    public static void encode(CAMQPEncoder encoder, CAMQPControlEnd data)
    {
        long listSize = 1;
        encoder.writeListDescriptor(descriptor, listSize);
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
    public static CAMQPControlEnd decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 1);
        CAMQPControlEnd data = new CAMQPControlEnd();

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
