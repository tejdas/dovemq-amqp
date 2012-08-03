package net.dovemq.transport.protocol;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPFormatCodes;
import net.dovemq.transport.protocol.data.CAMQPTypes;

public class CAMQPSyncDecoder
{
    private ChannelBuffer buffer = null;

    private boolean enoughDataAvailable = false;

    public static CAMQPSyncDecoder createCAMQPSyncDecoder()
    {
        return new CAMQPSyncDecoder();
    }

    private CAMQPSyncDecoder()
    {
    }

    public CAMQPMessagePayload getPayload()
    {
        byte[] payloadBody = new byte[buffer.readableBytes()];
        //buffer.getBytes(0, payloadBody);
        buffer.readBytes(payloadBody);
        return new CAMQPMessagePayload(payloadBody);
    }
    
    public boolean isEnoughDataAvailable()
    {
        return enoughDataAvailable;
    }

    public void take(ChannelBuffer bufferReceived)
    {
        enoughDataAvailable = true;
        if (buffer == null)
        {
            buffer = bufferReceived;
        }
        else
        {
            buffer = ChannelBuffers.wrappedBuffer(buffer, bufferReceived);
        }
    }

    private void checkEnoughBytesAvailable(int width)
    {
        if (buffer.readableBytes() < width)
        {
            enoughDataAvailable = false;
            throw new InsufficientBytesException();
        }
    }

    public int readFormatCode()
    {
        checkEnoughBytesAvailable(1);
        return CAMQPCodecUtil.readFormatCode(buffer);
    }

    public int readUByte()
    {
        checkEnoughBytesAvailable(Width.FIXED_ONE.widthOctets());
        return CAMQPCodecUtil.readUByte(buffer);
    }

    public int readUShort()
    {
        checkEnoughBytesAvailable(Width.FIXED_TWO.widthOctets());
        return CAMQPCodecUtil.readUShort(buffer);
    }

    public long readUInt()
    {
        checkEnoughBytesAvailable(Width.FIXED_FOUR.widthOctets());
        return CAMQPCodecUtil.readUInt(buffer);
    }

    public BigInteger readULong()
    {
        // REVISIT TODO correct?
        long val = readLong();
        return BigInteger.valueOf(val);
    }

    public byte readByte()
    {
        checkEnoughBytesAvailable(Width.FIXED_ONE.widthOctets());
        return CAMQPCodecUtil.readByte(buffer);
    }

    public short readShort()
    {
        checkEnoughBytesAvailable(Width.FIXED_TWO.widthOctets());
        return CAMQPCodecUtil.readShort(buffer);
    }

    public int readInt()
    {
        checkEnoughBytesAvailable(Width.FIXED_FOUR.widthOctets());
        return CAMQPCodecUtil.readInt(buffer);
    }

    public long readLong()
    {
        checkEnoughBytesAvailable(Width.FIXED_EIGHT.widthOctets());
        return CAMQPCodecUtil.readLong(buffer);
    }

    public float readFloat()
    {
        checkEnoughBytesAvailable(Width.FIXED_FOUR.widthOctets());
        return CAMQPCodecUtil.readFloat(buffer);
    }

    public double readDouble()
    {
        checkEnoughBytesAvailable(Width.FIXED_EIGHT.widthOctets());
        return CAMQPCodecUtil.readDouble(buffer);
    }

    public Date readTimeStamp()
    {
        checkEnoughBytesAvailable(Width.FIXED_EIGHT.widthOctets());
        return CAMQPCodecUtil.readTimeStamp(buffer);
    }

    public UUID readUUID()
    {
        checkEnoughBytesAvailable(Width.FIXED_SIXTEEN.widthOctets());
        return CAMQPCodecUtil.readUUID(buffer);
    }

    public long readBinaryDataSize(int formatCode)
    {
        if (formatCode == CAMQPFormatCodes.VBIN8)
        {
            checkEnoughBytesAvailable(Width.VARIABLE_ONE.widthOctets());
            return CAMQPCodecUtil.readUByte(buffer);
        }
        else if (formatCode == CAMQPFormatCodes.VBIN32)
        {
            checkEnoughBytesAvailable(Width.VARIABLE_FOUR.widthOctets());
            return CAMQPCodecUtil.readUInt(buffer);
        }
        else
        {
            return -1; // REVISIT
        }
    }

    public ChannelBuffer readBinary(int formatCode, long size, boolean copyFree)
    {
        return CAMQPSyncBinaryDataParser.parseBinaryData(buffer, size, copyFree);
    }

    public String readString(int formatCode)
    {
        long size = 0;
        String charSet = CAMQPProtocolConstants.CHARSET_UTF8;
        if (Width.VARIABLE_ONE.widthOctets() == CAMQPCodecUtil.computeWidth(formatCode))
        {
            checkEnoughBytesAvailable(Width.VARIABLE_ONE.widthOctets());
            size = CAMQPCodecUtil.readUByte(buffer);
        }
        else
        {
            checkEnoughBytesAvailable(Width.VARIABLE_FOUR.widthOctets());
            size = CAMQPCodecUtil.readUInt(buffer);
        }

        long parsedSoFar = 0;
        ChannelBuffer channelStrBuf = null;
        while (parsedSoFar < size)
        {
            boolean copyFree = false;
            ChannelBuffer buf = CAMQPSyncBinaryDataParser.parseBinaryData(buffer, (size - parsedSoFar), copyFree);
            parsedSoFar += buf.readableBytes();
            if (channelStrBuf == null)
            {
                channelStrBuf = buf;
            }
            else
            {
                channelStrBuf = ChannelBuffers.wrappedBuffer(channelStrBuf, buf);
            }
        }

        byte[] strBytes = new byte[channelStrBuf.readableBytes()];
        channelStrBuf.getBytes(0, strBytes);

        try
        {
            return new String(strBytes, charSet);
        }
        catch (UnsupportedEncodingException e)
        {
            // REVISIT TODO tejdas
            throw new CAMQPCodecException(CAMQPTypes.STR8_UTF8, formatCode);
        }
    }

    public boolean isNextDescribedConstructor()
    {
        return (buffer.getByte(buffer.readerIndex()) == 0);
    }

    public String readSymbol()
    {
        int firstByte = CAMQPCodecUtil.readUByte(buffer);
        assert (firstByte == 0);
        int formatCode = CAMQPCodecUtil.readFormatCode(buffer);
        assert ((formatCode == CAMQPFormatCodes.SYM8) || (formatCode == CAMQPFormatCodes.SYM32));
        return readString(formatCode);
    }

    public long readCompoundSize(int formatCode)
    {
        // size of the composite structure: for now skip it:REVISIT TODO
        int width = CAMQPCodecUtil.computeWidth(formatCode);
        buffer.skipBytes(width);
        if (Width.VARIABLE_ONE.widthOctets() == width)
        {
            return CAMQPCodecUtil.readUByte(buffer);
        }
        else
        // (width == Width.VARIABLE_FOUR)
        {
            return CAMQPCodecUtil.readUInt(buffer);
        }
    }

    public long readMapCount(int formatCode)
    {
        return readCompoundSize(formatCode) / 2;
    }

    public long readArrayCount(int formatCode)
    {
        return readCompoundSize(formatCode);
    }

    public CAMQPCompundHeader readMultipleElementCount()
    {
        int elementFormatCode;
        if (isNextDescribedConstructor())
        {
            int firstByte = CAMQPCodecUtil.readUByte(buffer);
            assert (firstByte == 0);
            assert (CAMQPFormatCodes.TRUE == CAMQPCodecUtil.readFormatCode(buffer));
            int compoundFormatCode = CAMQPCodecUtil.readFormatCode(buffer);
            long compoundCount = readCompoundSize(compoundFormatCode);
            elementFormatCode = CAMQPCodecUtil.readFormatCode(buffer);
            return new CAMQPCompundHeader(elementFormatCode, compoundCount);
        }
        else
        {
            elementFormatCode = CAMQPCodecUtil.readFormatCode(buffer);
            if (elementFormatCode == CAMQPFormatCodes.NULL)
            {
                return new CAMQPCompundHeader(CAMQPFormatCodes.NULL, 0);
            }
            else
            {
                return new CAMQPCompundHeader(elementFormatCode, 1);
            }
        }
    }
}
