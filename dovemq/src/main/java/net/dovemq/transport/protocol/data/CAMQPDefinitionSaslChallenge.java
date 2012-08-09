/**
 * This file was auto-generated by dovemq gentools.
 * Do not modify.
 */
package net.dovemq.transport.protocol.data;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionSaslChallenge
{
    public static final String descriptor = "amqp:sasl-challenge:list";

    private byte[] challenge = null;
    public void setChallenge(byte[] val)
    {
        challenge = val;
    }

    public byte[] getChallenge()
    {
        return challenge;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionSaslChallenge data)
    {
        long listSize = 1;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.challenge != null)
        {
            encoder.writeBinary(data.challenge, data.challenge.length, false);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionSaslChallenge decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 1);
        CAMQPDefinitionSaslChallenge data = new CAMQPDefinitionSaslChallenge();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.challenge = new byte[size];
            channelBuf.readBytes(data.challenge);
        }
        return data;
    }
}