package net.dovemq.transport.protocol.data;

import java.util.ArrayList;
import java.util.Collection;
import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionSaslMechanisms
{
    public static final String descriptor = "amqp:sasl-mechanisms:list";

    private Collection<String> saslServerMechanisms = new ArrayList<String>();
    public void addSaslServerMechanisms(String val)
    {
        saslServerMechanisms.add(val);
    }

    public Collection<String> getSaslServerMechanisms()
    {
        return saslServerMechanisms;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionSaslMechanisms data)
    {
        long listSize = 1;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.saslServerMechanisms != null)
        {
            long saslServerMechanismsSize = data.saslServerMechanisms.size();
            if (saslServerMechanismsSize > 0)
            {
                encoder.writeArrayHeaderForMultiple(saslServerMechanismsSize, CAMQPFormatCodes.SYM8);
                for (String val : data.saslServerMechanisms)
                {
                    encoder.writeSymbolArrayElement(val);
                }
                encoder.fillCompoundSize(saslServerMechanismsSize);
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
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionSaslMechanisms decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 1);
        CAMQPDefinitionSaslMechanisms data = new CAMQPDefinitionSaslMechanisms();

        {
            CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();
            for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)
            {
                data.saslServerMechanisms.add(decoder.readString(compoundHeader.elementFormatCode));
            }
        }
        return data;
    }
}
