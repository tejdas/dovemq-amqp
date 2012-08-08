package net.dovemq.transport.protocol;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Stack;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPFormatCodes;
import net.dovemq.transport.protocol.data.CAMQPTypes;

public class CAMQPEncoder
{
    private ChannelBuffer buffer = null;
    private ChannelBuffer dynamicBuffer = null;
    private boolean isComposite = false;
    private final Stack<Integer> compoundSizePosition = new Stack<Integer>();
    
    public static CAMQPEncoder createCAMQPEncoder()
    {
        return new CAMQPEncoder();
    }
    
    private CAMQPEncoder()
    {
    }
    
    public ChannelBuffer getEncodedBuffer()
    {
        ChannelBuffer flushedBuffer;
        if (dynamicBuffer != null)
        {
            flushedBuffer = ChannelBuffers.wrappedBuffer(buffer, dynamicBuffer);
            dynamicBuffer = null;
        }
        else
        {
            flushedBuffer = buffer;
        }

        buffer = null;
        isComposite = false;
        assert(compoundSizePosition.empty());
        return flushedBuffer;
    }

    private ChannelBuffer getWritableBuffer()
    {
        if (dynamicBuffer == null)
        {
            if (buffer == null)
            {
                buffer = ChannelBuffers.dynamicBuffer(CAMQPProtocolConstants.DYNAMIC_BUFFER_INITIAL_SIZE, new HeapChannelBufferFactory());
                return buffer;
            }
            else
            {
                if (isComposite)
                {
                    dynamicBuffer = ChannelBuffers.dynamicBuffer(CAMQPProtocolConstants.DYNAMIC_BUFFER_INITIAL_SIZE, new HeapChannelBufferFactory());
                    return dynamicBuffer;
                }
                else
                {
                    return buffer;
                }
            }
        }
        else
        {
            return dynamicBuffer;
        }
    }
    
    private ChannelBuffer ensureCapacity(long size)
    {
        ChannelBuffer currentBuffer =
                (dynamicBuffer != null) ? dynamicBuffer : buffer;

        if (size <= (CAMQPProtocolConstants.INT_MAX_VALUE - currentBuffer.capacity()))
        {
            return currentBuffer;
        }
        if (dynamicBuffer != null)
        {
            buffer = ChannelBuffers.wrappedBuffer(buffer, dynamicBuffer);
        }
        dynamicBuffer =
                ChannelBuffers.dynamicBuffer(
                        CAMQPProtocolConstants.DYNAMIC_BUFFER_INITIAL_SIZE,
                        new HeapChannelBufferFactory());
        return dynamicBuffer;
    }
    
    public void writePayload(CAMQPMessagePayload payload)
    {
        byte[] payloadBody = payload.getPayload();
        writePayloadInternal(payloadBody);
    }
    
    public void writeNull()
    {
        byte formatCode = (byte) CAMQPFormatCodes.NULL;
        getWritableBuffer().writeByte(formatCode);
    }
    
    public void writeBoolean(boolean value)
    {
        byte formatCode = value? (byte) CAMQPFormatCodes.TRUE : (byte) CAMQPFormatCodes.FALSE;
        getWritableBuffer().writeByte(formatCode);
    }    
    
    public void writeUByte(int value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();  
        byte formatCode = (byte) CAMQPFormatCodes.UBYTE;
        writableBuffer.writeByte(formatCode);
        CAMQPCodecUtil.writeUByte(value, writableBuffer);
    }    
    
    public void writeUShort(int value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode = (byte) CAMQPFormatCodes.USHORT;
        writableBuffer.writeByte(formatCode);
        CAMQPCodecUtil.writeUShort(value, writableBuffer);
    }    
    
    public void writeUInt(long value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        byte formatCode = (byte) CAMQPFormatCodes.UINT;
        writableBuffer.writeByte(formatCode);
        CAMQPCodecUtil.writeUInt(value, writableBuffer);
    }
    
    public void writeByte(byte value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        byte formatCode = (byte) CAMQPFormatCodes.BYTE;
        writableBuffer.writeByte(formatCode);
        writableBuffer.writeByte(value);        
    }
    
    public void
    writeShort(short value)
    {
        byte formatCode = (byte) CAMQPFormatCodes.SHORT;
        ChannelBuffer writableBuffer = getWritableBuffer();
        writableBuffer.writeByte(formatCode);
        writableBuffer.writeShort(value);
    }
    
    public void writeInt(int value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode = (byte) CAMQPFormatCodes.INT;
        writableBuffer.writeByte(formatCode);
        writableBuffer.writeInt(value);
    }    
    
    public void writeULong(BigInteger value)
    {
        writeLong(value.longValue()); // REVISIT TODO correct?
    }    
    
    public void writeLong(long value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer(); 
        byte formatCode = (byte) CAMQPFormatCodes.LONG;
        writableBuffer.writeByte(formatCode);
        writableBuffer.writeLong(value);
    }    
    
    public void writeFloat(float value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode = (byte) CAMQPFormatCodes.FLOAT;
        writableBuffer.writeByte(formatCode);
        int floatAsInt = Float.floatToIntBits(value);
        writableBuffer.writeInt(floatAsInt);
    }    
    
    public void writeDouble(double value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode = (byte) CAMQPFormatCodes.DOUBLE;
        writableBuffer.writeByte(formatCode);
        long doubleAsLong = Double.doubleToLongBits(value);
        writableBuffer.writeLong(doubleAsLong);
    }
    
    public void writeTimeStamp(Date value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        byte formatCode = (byte) CAMQPFormatCodes.TIMESTAMP;
        writableBuffer.writeByte(formatCode);
        long milliSeconds = value.getTime();
        writableBuffer.writeLong(milliSeconds);
    }    
    
    public void writeUUID(UUID value)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        byte formatCode = (byte) CAMQPFormatCodes.UUID;
        writableBuffer.writeByte(formatCode);
        long msb = value.getMostSignificantBits();
        long lsb = value.getLeastSignificantBits();        
        writableBuffer.writeLong(msb);
        writableBuffer.writeLong(lsb);        
    }
    
    public void writeUTF8String(String str)
    {
        boolean isSymbol = false;   
        boolean isSelfDescribed = true;
        writeString(str, CAMQPProtocolConstants.CHARSET_UTF8, isSymbol, isSelfDescribed);
    }
    
    public void writeUTF16String(String str)
    {
        boolean isSymbol = false;      
        boolean isSelfDescribed = true;
        writeString(str, CAMQPProtocolConstants.CHARSET_UTF16, isSymbol, isSelfDescribed);
    }

    public void writeSymbol(String symbol)
    {
        boolean isSymbol = true;
        boolean isSelfDescribed = true;
        writeString(symbol, CAMQPProtocolConstants.CHARSET_UTF8, isSymbol, isSelfDescribed);
    }
    
    public void writeUTF8StringArrayElement(String str)
    {
        boolean isSymbol = false;   
        boolean isSelfDescribed = false;
        writeString(str, CAMQPProtocolConstants.CHARSET_UTF8, isSymbol, isSelfDescribed);
    }
    
    public void writeUTF16StringArrayElement(String str)
    {
        boolean isSymbol = false;      
        boolean isSelfDescribed = false;
        writeString(str, CAMQPProtocolConstants.CHARSET_UTF16, isSymbol, isSelfDescribed);
    }

    public void writeSymbolArrayElement(String symbol)
    {
        boolean isSymbol = true;
        boolean isSelfDescribed = false;
        writeString(symbol, CAMQPProtocolConstants.CHARSET_UTF8, isSymbol, isSelfDescribed);
    }    

    private void writeString(String str, String charSet, boolean isSymbol, boolean isSelfDescribed)
    {
        byte[] encodedString;
        try
        {
            encodedString = str.getBytes(charSet);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new CAMQPCodecException(CAMQPTypes.STR8_UTF8, CAMQPFormatCodes.STR8_UTF8);
        }
        long size = encodedString.length;

        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode;
        if (size <= CAMQPProtocolConstants.UBYTE_MAX_VALUE)
        {
            if (isSelfDescribed)
            {
                if (isSymbol)
                {
                    formatCode = (byte) CAMQPFormatCodes.SYM8;
                }
                else
                {
                    // REVISIT TODO
                    //formatCode = (charSet.equalsIgnoreCase(CAMQPProtocolConstants.CHARSET_UTF8)) ? (byte) CAMQPFormatCodes.STR8_UTF8 : (byte) CAMQPFormatCodes.STR8_UTF32;
                    formatCode = (byte) CAMQPFormatCodes.STR8_UTF8;
                }
                writableBuffer.writeByte(formatCode);
            }
            CAMQPCodecUtil.writeUByte((int) size, writableBuffer);

            /*
             * Ignore copyFree and ALWAYS deep-copy
             */
            ChannelBuffer bufferToCopy = ensureCapacity(size);
            bufferToCopy.writeBytes(encodedString, 0, (int) size);
        }
        else if (size <= CAMQPProtocolConstants.UINT_MAX_VALUE)
        {
            if (isSelfDescribed)
            {
                if (isSymbol)
                {
                    formatCode = (byte) CAMQPFormatCodes.SYM32;
                }
                else
                {
                    // REVISIT TODO
                    //formatCode = (charSet.equalsIgnoreCase(CAMQPProtocolConstants.CHARSET_UTF8)) ? (byte) CAMQPFormatCodes.STR32_UTF8 : (byte) CAMQPFormatCodes.STR32_UTF16;
                    formatCode = (byte) CAMQPFormatCodes.STR32_UTF8;
                }
                writableBuffer.writeByte(formatCode);
            }
            CAMQPCodecUtil.writeUInt(size, writableBuffer);
            ChannelBuffer wrappedBinaryData =
                    ChannelBuffers.wrappedBuffer(encodedString);
            if (dynamicBuffer != null)
            {
                buffer = ChannelBuffers.wrappedBuffer(buffer, dynamicBuffer, wrappedBinaryData);
                dynamicBuffer = null;
            } else
            {
                buffer = ChannelBuffers.wrappedBuffer(buffer, wrappedBinaryData);
            }
            isComposite = true;
        } else
        {
            // error condition
            return;
        }
    }

    /*
     * Adopts the buffer containing binaryData, if copyFree
     * is true.
     * 
     * FormatCodes.VBIN8 : Ignores copyFree and ALWAYS deep-copy
     * the buffer.
     * 
     * FormatCodes.VBIN16 : If copyFree is true, then adopts the buffer
     * as ChannelBuffers.wrappedBuffer. Otherwise, deep-copies the buffer
     * into DynamicChannelBuffer.
     * 
     * FormatCodes.VBIN32 : If copyFree is true, then adopts the buffer
     * as ChannelBuffers.wrappedBuffer. Otherwise, deep-copies into a
     * stand-alone ChannelBuffer and creates a wrappedBuffer.
     */
    public void
    writeBinary(byte[] binaryData, long size, boolean copyFree)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();
        byte formatCode;
        if (size <= CAMQPProtocolConstants.UBYTE_MAX_VALUE)
        {
            formatCode = (byte) CAMQPFormatCodes.VBIN8;
            writableBuffer.writeByte(formatCode);
            CAMQPCodecUtil.writeUByte((int) size, writableBuffer);

            /*
             * Ignore copyFree and ALWAYS deep-copy
             */
            ChannelBuffer bufferToCopy = ensureCapacity(size);
            bufferToCopy.writeBytes(binaryData, 0, (int) size);
            return;
        }
        else if (size <= CAMQPProtocolConstants.UINT_MAX_VALUE)
        {
            formatCode = (byte) CAMQPFormatCodes.VBIN32;
            writableBuffer.writeByte(formatCode);
            CAMQPCodecUtil.writeUInt(size, writableBuffer);            
        }
        else
        {
            // error condition
            return;
        }

        writeBinaryBody(binaryData, copyFree);
    }
    
    private void
    writePayloadInternal(byte[] binaryData)
    {
        int size = binaryData.length;
        if (size <= CAMQPProtocolConstants.UBYTE_MAX_VALUE)
        {
            /*
             * Ignore copyFree and ALWAYS deep-copy
             */
            ChannelBuffer bufferToCopy = ensureCapacity(size);
            bufferToCopy.writeBytes(binaryData, 0, (int) size);
            return;
        }
        else if (size <= CAMQPProtocolConstants.UINT_MAX_VALUE)
        {
            writeBinaryBody(binaryData, true);
        }
        else
        {
            // error condition
            return;
        }
    }

    private void writeBinaryBody(byte[] binaryData, boolean copyFree)
    {
        ChannelBuffer wrappedBinaryData;
        if (copyFree)
        {
            wrappedBinaryData = ChannelBuffers.wrappedBuffer(binaryData);
        }
        else
        {
            //assert(size > CAMQPProtocolConstants.USHORT_MAX_VALUE); // REVISIT TODO
            wrappedBinaryData = ChannelBuffers.copiedBuffer(binaryData);
        }

        if (dynamicBuffer != null)
        {
            buffer = ChannelBuffers.wrappedBuffer(buffer, dynamicBuffer, wrappedBinaryData);
            dynamicBuffer = null;
        }
        else
        {
            buffer = ChannelBuffers.wrappedBuffer(buffer, wrappedBinaryData);
        }
        isComposite = true;
    }
    
    public void  writeNumericDescriptor(long domainID, long descriptorID)
    {
        writeUInt((domainID << 32) | descriptorID);        
    }
    
    private void writeSymbolicConstructor(String symbol, int formatCode)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        CAMQPCodecUtil.writeUByte(0, writableBuffer);
        writeSymbol(symbol);
        getWritableBuffer().writeByte((byte) formatCode);        
    }
    
    public void writeListDescriptor(String symbol, long listCount)
    {
        int formatCode = (listCount <= 255)? CAMQPFormatCodes.LIST8 : CAMQPFormatCodes.LIST32;
        writeCompoundDescriptor(symbol, formatCode, listCount);        
    }
    
    public void writeMapDescriptor(String symbol, long mapCount)
    {
        int formatCode = (mapCount <= 255)? CAMQPFormatCodes.MAP8 : CAMQPFormatCodes.MAP32;
        writeCompoundDescriptor(symbol, formatCode, mapCount * 2);        
    }
    
    public void writeArrayDescriptor(String symbol, long arrayCount)
    {
        int formatCode = CAMQPFormatCodes.ARRAY8; // April 21 2011
        writeCompoundDescriptor(symbol, formatCode, arrayCount);        
    }
    
    private void writeCompoundDescriptor(String symbol, int formatCode, long compoundCount)
    {
        writeSymbolicConstructor(symbol, formatCode);
        writeCompoundHeader(formatCode, compoundCount);
    }
    
    public void writePrimitiveDescriptor(int primitiveFormatCode)
    {
        ChannelBuffer writableBuffer = getWritableBuffer();        
        CAMQPCodecUtil.writeUByte(0, writableBuffer);
        getWritableBuffer().writeByte((byte) primitiveFormatCode);
    }    

    // REFACTOR TODO
    public void writeListHeaderForMultiple(long listCount, int listElementFormatCode)
    {
        /*
         * DescriptorForMultipleTrue ListFormatCode ListSize ListCount ListElementFormatCode
         * 
         * For example of LIST8  of symbols (SYM8) and size 0x57 and count 3 it would look like the following:
         * 00 41 C0 57 03 A3
         */
        if (listCount > 1)
        {
            int formatCode = (listCount <= 255) ? CAMQPFormatCodes.LIST8 : CAMQPFormatCodes.LIST32;
            ChannelBuffer writableBuffer = getWritableBuffer();
            CAMQPCodecUtil.writeUByte(0, writableBuffer);
            getWritableBuffer().writeByte(CAMQPFormatCodes.TRUE);
            getWritableBuffer().writeByte((byte) formatCode);
            writeCompoundHeader(formatCode, listCount);
            getWritableBuffer().writeByte((byte) listElementFormatCode);
        }
        else
        {
            getWritableBuffer().writeByte((byte) listElementFormatCode);
        }
    }
    
    public void writeListHeaderArrayElement(long listCount)
    {
        int formatCode = (listCount <= 255)? CAMQPFormatCodes.LIST8 : CAMQPFormatCodes.LIST32;
        writeCompoundHeader(formatCode, listCount);
    }    
    
    public void writeArrayHeader(long arrayCount, int arrayElementFormatCode)
    {
        int formatCode = (arrayCount <= 255)? CAMQPFormatCodes.ARRAY8 : CAMQPFormatCodes.ARRAY32;
        getWritableBuffer().writeByte((byte) formatCode);
        writeCompoundHeader(formatCode, arrayCount);
        getWritableBuffer().writeByte((byte) arrayElementFormatCode);
    }
    
    // REFACTOR TODO
    public void writeArrayHeaderForMultiple(long arrayCount, int arrayElementFormatCode)
    {
        /*
         * DescriptorForMultipleTrue ArrayFormatCode ArraySize ArrayCount ArrayElementFormatCode
         * 
         * For example of ARRAY8  of symbols (SYM8) and size 0x57 and count 3 it would look like the following:
         * 00 41 E0 57 03 A3
         */
        if (arrayCount > 1)
        {
            int formatCode = (arrayCount <= 255) ? CAMQPFormatCodes.ARRAY8 : CAMQPFormatCodes.ARRAY32;
            ChannelBuffer writableBuffer = getWritableBuffer();
            CAMQPCodecUtil.writeUByte(0, writableBuffer);
            getWritableBuffer().writeByte(CAMQPFormatCodes.TRUE);
            getWritableBuffer().writeByte((byte) formatCode);
            writeCompoundHeader(formatCode, arrayCount);
            getWritableBuffer().writeByte((byte) arrayElementFormatCode);
        }
        else
        {
            getWritableBuffer().writeByte((byte) arrayElementFormatCode);
        }
    }    
    
    public void writeMapHeader(long mapCount)
    {
        int formatCode = (mapCount <= 255)? CAMQPFormatCodes.MAP8 : CAMQPFormatCodes.MAP32;
        getWritableBuffer().writeByte((byte) formatCode);
        writeCompoundHeader(formatCode, mapCount * 2);
    }
    
    public void fillCompoundSize(long compoundCount)
    {
        if (compoundSizePosition.size() == 0)
        {
            return;
        }
        
        int width =(compoundCount <= 255)? Width.FIXED_ONE.widthOctets() : Width.FIXED_FOUR.widthOctets();
        int position = compoundSizePosition.pop();
        /*
         * We need to exclude the size field from the compound size.
         */
        long compoundSize = getWritableBuffer().readableBytes() - (position + width);
        getWritableBuffer().markWriterIndex();
        
        if (width == 1)
        {
            CAMQPCodecUtil.writeUByteAt((int) compoundSize, position, getWritableBuffer());
        }
        else // (width == 4)
        {
            CAMQPCodecUtil.writeUIntAt(compoundSize, position, getWritableBuffer());
        }        
        getWritableBuffer().resetWriterIndex();
    }
    
    private void writeCompoundHeader(int formatCode, long compoundCount)
    {
        int width = CAMQPCodecUtil.computeWidth(formatCode);
        compoundSizePosition.push(getWritableBuffer().readableBytes());
        if (width == 1)
        {
            // size of the composite structure: for now set to 0 REVISIT TODO
            CAMQPCodecUtil.writeUByte((int) 0, getWritableBuffer());            
            CAMQPCodecUtil.writeUByte((int) compoundCount, getWritableBuffer());
        }
        else // (width == 4)
        {
            // size of the composite structure: for now set to 0 REVISIT TODO            
            CAMQPCodecUtil.writeUInt(0, getWritableBuffer());            
            CAMQPCodecUtil.writeUInt(compoundCount, getWritableBuffer());
        }
    }
}
