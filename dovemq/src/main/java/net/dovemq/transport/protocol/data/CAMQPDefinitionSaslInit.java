package net.dovemq.transport.protocol.data;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionSaslInit
{
    public static final String descriptor = "amqp:sasl-init:list";

    private String mechanism = null;
    public void setMechanism(String val)
    {
        mechanism = val;
    }

    public String getMechanism()
    {
        return mechanism;
    }

    private boolean isSetInitialResponse = false;
    public void setRequiredInitialResponse(boolean val)
    {
        isSetInitialResponse = val;
    }
    public boolean isSetInitialResponse()
    {
        return isSetInitialResponse;
    }
    private byte[] initialResponse = null;
    public void setInitialResponse(byte[] val)
    {
        isSetInitialResponse = true;
        initialResponse = val;
    }

    public byte[] getInitialResponse()
    {
        return initialResponse;
    }

    private boolean isSetHostname = false;
    public void setRequiredHostname(boolean val)
    {
        isSetHostname = val;
    }
    public boolean isSetHostname()
    {
        return isSetHostname;
    }
    private String hostname = null;
    public void setHostname(String val)
    {
        isSetHostname = true;
        hostname = val;
    }

    public String getHostname()
    {
        return hostname;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionSaslInit data)
    {
        long listSize = 3;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.mechanism != null)
        {
            encoder.writeSymbol(data.mechanism);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.initialResponse != null) && (data.isSetInitialResponse))
        {
            encoder.writeBinary(data.initialResponse, data.initialResponse.length, false);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.hostname != null) && (data.isSetHostname))
        {
            encoder.writeUTF8String(data.hostname);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionSaslInit decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 3);
        CAMQPDefinitionSaslInit data = new CAMQPDefinitionSaslInit();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.mechanism = decoder.readString(formatCode);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.initialResponse = new byte[size];
            channelBuf.readBytes(data.initialResponse);
            data.isSetInitialResponse = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.hostname = decoder.readString(formatCode);
            data.isSetHostname = true;
        }
        return data;
    }
}
