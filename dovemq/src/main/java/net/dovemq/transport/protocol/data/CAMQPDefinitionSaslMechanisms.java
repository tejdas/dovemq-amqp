/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * This file was auto-generated by dovemq gentools.
 * Do not modify.
 */
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
