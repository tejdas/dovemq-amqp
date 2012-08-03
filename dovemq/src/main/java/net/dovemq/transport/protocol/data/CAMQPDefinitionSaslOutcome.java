package net.dovemq.transport.protocol.data;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionSaslOutcome
{
    public static final String descriptor = "amqp:sasl-outcome:list";

    private int code = 0;
    public void setCode(int val)
    {
        code = val;
    }

    public int getCode()
    {
        return code;
    }

    private boolean isSetAdditionalData = false;
    public void setRequiredAdditionalData(boolean val)
    {
        isSetAdditionalData = val;
    }
    public boolean isSetAdditionalData()
    {
        return isSetAdditionalData;
    }
    private byte[] additionalData = null;
    public void setAdditionalData(byte[] val)
    {
        isSetAdditionalData = true;
        additionalData = val;
    }

    public byte[] getAdditionalData()
    {
        return additionalData;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionSaslOutcome data)
    {
        long listSize = 2;
        encoder.writeListDescriptor(descriptor, listSize);

        encoder.writeUByte(data.code);

        if ((data.additionalData != null) && (data.isSetAdditionalData))
        {
            encoder.writeBinary(data.additionalData, data.additionalData.length, false);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionSaslOutcome decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 2);
        CAMQPDefinitionSaslOutcome data = new CAMQPDefinitionSaslOutcome();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.code = decoder.readUByte();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.additionalData = new byte[size];
            channelBuf.readBytes(data.additionalData);
            data.isSetAdditionalData = true;
        }
        return data;
    }
}
