package net.dovemq.transport.protocol.data;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionSaslResponse
{
    public static final String descriptor = "amqp:sasl-response:list";

    private byte[] response = null;
    public void setResponse(byte[] val)
    {
        response = val;
    }

    public byte[] getResponse()
    {
        return response;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionSaslResponse data)
    {
        long listSize = 1;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.response != null)
        {
            encoder.writeBinary(data.response, data.response.length, false);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionSaslResponse decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 1);
        CAMQPDefinitionSaslResponse data = new CAMQPDefinitionSaslResponse();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.response = new byte[size];
            channelBuf.readBytes(data.response);
        }
        return data;
    }
}
