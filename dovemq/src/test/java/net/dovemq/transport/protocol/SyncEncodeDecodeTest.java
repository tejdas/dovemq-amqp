package net.dovemq.transport.protocol;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.data.CAMQPFormatCodes;

import junit.framework.TestCase;

public class SyncEncodeDecodeTest extends TestCase
{
    private static final String testFileName = System.getenv("HOME") + "/camqptest.jar";
    
    public SyncEncodeDecodeTest()
    {
    }

    @Override
    protected void setUp() throws Exception
    {
    }

    @Override
    protected void tearDown() throws Exception
    {
    }

    public void testFoo() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeNull();
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        buffer.markWriterIndex();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.NULL == inputPipe.readFormatCode());
    }
    
    public void testEncodeDecodeNULL() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeNull();
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.NULL == inputPipe.readFormatCode());
    }

    public void testEncodeDecodeBoolean() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeBoolean(true);
        outstream.writeBoolean(false);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.TRUE == inputPipe.readFormatCode());
        assertTrue(CAMQPFormatCodes.FALSE == inputPipe.readFormatCode());
    }

    public void testEncodeDecodeUByte() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeUByte(135);
        outstream.writeUByte(4075); // Negative test
        outstream.writeUByte(242);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.UBYTE == inputPipe.readFormatCode());
        assertEquals(135, inputPipe.readUByte());
        assertTrue(CAMQPFormatCodes.UBYTE == inputPipe.readFormatCode());
        assertFalse(4075 == inputPipe.readUByte());
        assertTrue(CAMQPFormatCodes.UBYTE == inputPipe.readFormatCode());
        assertEquals(242, inputPipe.readUByte());
    }

    public void testEncodeDecodeUShort() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeUShort(65000);
        outstream.writeUShort(66000); // Negative test
        outstream.writeUShort(6789);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.USHORT == inputPipe.readFormatCode());
        assertEquals(65000, inputPipe.readUShort());
        assertTrue(CAMQPFormatCodes.USHORT == inputPipe.readFormatCode());
        assertFalse(66000 == inputPipe.readUShort());
        assertTrue(CAMQPFormatCodes.USHORT == inputPipe.readFormatCode());
        assertEquals(6789, inputPipe.readUShort());
    }

    public void testEncodeDecodeUInt() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        long val = 4294967290L;
        outstream.writeUInt(val);
        // Negative test
        long val2 = 4394967290L;
        outstream.writeUInt(val2);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.UINT == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readUInt());
        assertTrue(CAMQPFormatCodes.UINT == inputPipe.readFormatCode());
        assertFalse(val2 == inputPipe.readUInt());
    }
    
    public void testEncodeDecodeBigInteger() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        outstream.writeULong(BigInteger.valueOf(8746L));
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);

        assertTrue(CAMQPFormatCodes.LONG == inputPipe.readFormatCode());
        BigInteger output = inputPipe.readULong();
        assertEquals(output.longValue(), 8746L);
    }    

    public void testEncodeDecodeByte() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        byte val = 124;
        outstream.writeByte(val);
        byte val1 = -75;
        outstream.writeByte(val1);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.BYTE == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readByte());
        assertTrue(CAMQPFormatCodes.BYTE == inputPipe.readFormatCode());
        assertEquals(val1, inputPipe.readByte());
    }

    public void testEncodeDecodeShort() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        short val = 32667;
        outstream.writeShort(val);
        short val2 = -32455;
        outstream.writeShort(val2);
        // Negative test
        int val3 = 33800;
        outstream.writeShort((short) val3);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.SHORT == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readShort());
        assertTrue(CAMQPFormatCodes.SHORT == inputPipe.readFormatCode());
        assertEquals(val2, inputPipe.readShort());
        assertTrue(CAMQPFormatCodes.SHORT == inputPipe.readFormatCode());
        assertFalse(val3 == inputPipe.readShort());
    }

    public void testEncodeDecodeInt() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        int val = 2147483552;
        outstream.writeInt(val);
        int val2 = -2147483435;
        outstream.writeInt(val2);
        // Negative test
        long val3 = 2147483955L;
        outstream.writeInt((int) val3);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.INT == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readInt());
        assertTrue(CAMQPFormatCodes.INT == inputPipe.readFormatCode());
        assertEquals(val2, inputPipe.readInt());
        assertTrue(CAMQPFormatCodes.INT == inputPipe.readFormatCode());
        assertFalse(val3 == inputPipe.readInt());
    }

    public void testEncodeDecodeLong() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        long val = 9223372036854775778L;
        outstream.writeLong(val);
        long val2 = -9223372036854665807L;
        outstream.writeLong(val2);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.LONG == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readLong());
        assertTrue(CAMQPFormatCodes.LONG == inputPipe.readFormatCode());
        assertEquals(val2, inputPipe.readLong());
    }

    public void testEncodeDecodeFloat() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        float val = 9223372036854775778f;
        outstream.writeFloat(val);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.FLOAT == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readFloat());
    }

    public void testEncodeDecodeDouble() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        double val = 9223372036854775778d;
        outstream.writeDouble(val);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.DOUBLE == inputPipe.readFormatCode());
        assertEquals(val, inputPipe.readDouble());
    }

    public void testEncodeDecodeTimestamp() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        Date date = new Date();
        outstream.writeTimeStamp(date);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.TIMESTAMP == inputPipe.readFormatCode());
        assertEquals(date, inputPipe.readTimeStamp());
    }

    public void testEncodeDecodeUUID() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        UUID uuid = UUID.randomUUID();
        outstream.writeUUID(uuid);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        assertTrue(CAMQPFormatCodes.UUID == inputPipe.readFormatCode());
        assertEquals(uuid, inputPipe.readUUID());
    }
    
    public void testEncodeDecodeUTF8String() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        String str = "Milton Friedman";
        outstream.writeUTF8String(str);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        int formatCode = inputPipe.readFormatCode();
        assertTrue(CAMQPFormatCodes.STR8_UTF8 == formatCode);
        String str2 = inputPipe.readString(formatCode);
        assertEquals(str, str2);
    }

    public void testEncodeDecodeSym8() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        String str = "example:book:list";
        outstream.writeSymbol(str);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        int formatCode = inputPipe.readFormatCode();
        assertTrue(CAMQPFormatCodes.SYM8 == formatCode);
        String str2 = inputPipe.readString(formatCode);
        assertEquals(str, str2);
    }

    public void testEncodeDecodeBinary8MultipleTypes() throws Exception
    {
        testEncodeDecodeBinary(CAMQPFormatCodes.VBIN8, true, true, true);
    }

    public void testEncodeDecodeBinary32MultipleTypes() throws Exception
    {
        testEncodeDecodeBinary(CAMQPFormatCodes.VBIN32, true, true, true);
    }

    private void testEncodeDecodeBinary(int formatCode,
            boolean adoptOrCopyOnEncode, boolean adoptOrCopyOnDecode,
            boolean multipleDataTypes) throws Exception
    {
        Random rand = new Random();
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();

        int bufSize;
        if (formatCode == CAMQPFormatCodes.VBIN8)
            bufSize = 128;
        else
            bufSize = 262144;

        BufferedInputStream inputStream =
                new BufferedInputStream(new FileInputStream(testFileName));
        while (inputStream.available() > 0)
        {
            byte[] inbuf = new byte[bufSize];
            int bytesRead = inputStream.read(inbuf);
            if (bytesRead < bufSize)
            {
                byte[] inbuf2 = new byte[bytesRead];
                for (int i = 0; i < bytesRead; i++)
                {
                    inbuf2[i] = inbuf[i];
                }
                outstream.writeBinary(inbuf2, bytesRead, adoptOrCopyOnEncode);
            } else
                outstream.writeBinary(inbuf, bytesRead, adoptOrCopyOnEncode);

            long val = 0;
            int val2 = 0;
            float val3 = 0;
            double val4 = 0;
            boolean val5 = false;
            UUID uuid = UUID.randomUUID();
            Date date = new Date();
            if (multipleDataTypes)
            {
                val = rand.nextLong();
                outstream.writeLong(val);

                val2 = rand.nextInt();
                outstream.writeInt(val2);

                outstream.writeUUID(uuid);

                val3 = rand.nextFloat();
                outstream.writeFloat(val3);

                outstream.writeTimeStamp(date);

                val4 = rand.nextDouble();
                outstream.writeDouble(val4);

                val5 = rand.nextBoolean();
                outstream.writeBoolean(val5);
            }

            ChannelBuffer buffer = outstream.getEncodedBuffer();

            CAMQPSyncDecoder inputPipe =
                    CAMQPSyncDecoder
                            .createCAMQPSyncDecoder();
            inputPipe.take(buffer);

            int binaryDataformatCode = inputPipe.readFormatCode();
            assertTrue((binaryDataformatCode == CAMQPFormatCodes.VBIN8)
                    || (binaryDataformatCode == CAMQPFormatCodes.VBIN32));
            long bytesToRead = inputPipe.readBinaryDataSize(binaryDataformatCode);
            assertEquals(bytesRead, bytesToRead);
            while (bytesToRead > 0)
            {
                ChannelBuffer bufferRead =
                        inputPipe.readBinary(binaryDataformatCode, bytesToRead, adoptOrCopyOnDecode);
                bytesToRead -= bufferRead.readableBytes();
            }

            if (multipleDataTypes)
            {
                assertTrue(CAMQPFormatCodes.LONG == inputPipe.readFormatCode());
                assertEquals(val, inputPipe.readLong());
                assertTrue(CAMQPFormatCodes.INT == inputPipe.readFormatCode());
                assertEquals(val2, inputPipe.readInt());
                assertTrue(CAMQPFormatCodes.UUID == inputPipe.readFormatCode());
                assertEquals(uuid, inputPipe.readUUID());
                assertTrue(CAMQPFormatCodes.FLOAT == inputPipe.readFormatCode());
                assertEquals(val3, inputPipe.readFloat());
                assertTrue(CAMQPFormatCodes.TIMESTAMP == inputPipe.readFormatCode());
                assertEquals(date, inputPipe.readTimeStamp());
                assertTrue(CAMQPFormatCodes.DOUBLE == inputPipe.readFormatCode());
                assertEquals(val4, inputPipe.readDouble());
                if (val5)
                    assertTrue(CAMQPFormatCodes.TRUE == inputPipe
                            .readFormatCode());
                else
                    assertTrue(CAMQPFormatCodes.FALSE == inputPipe
                            .readFormatCode());
            }
        }
        inputStream.close();
    }

    public void
    testEncodeDecodeBinaryMultipleTypes() throws Exception
    {
        testEncodeDecodeBinaryAllFormatCodes(true, true, true);
    }

    public void
    testEncodeDecodeBinaryMultipleTypesCopyOnDecode() throws Exception
    {
        testEncodeDecodeBinaryAllFormatCodes(false, true, true);
    }
    
    public void
    testEncodeDecodeBinaryMultipleTypesCopyOnEncode() throws Exception
    {
        testEncodeDecodeBinaryAllFormatCodes(true, false, true);
    }
    
    public void
    testEncodeDecodeBinaryMultipleTypesCopyOnEncodeDecode() throws Exception
    {
        testEncodeDecodeBinaryAllFormatCodes(false, false, true);
    }
    
    private void testEncodeDecodeBinaryAllFormatCodes(
            boolean adoptOrCopyOnEncode, boolean adoptOrCopyOnDecode,
            boolean multipleDataTypes) throws Exception
    {
        int formatCodes[] = {CAMQPFormatCodes.VBIN8, CAMQPFormatCodes.VBIN32};
        Random rand = new Random();
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();

        BufferedInputStream inputStream =
                new BufferedInputStream(new FileInputStream(testFileName));
        int iter = 0;
        while (inputStream.available() > 0)
        {
            int formatCode = formatCodes[iter % 2];
            iter++;
            
            int bufSize;
            if (formatCode == CAMQPFormatCodes.VBIN8)
                bufSize = 128;
            else
                bufSize = 262144;
            
            byte[] inbuf = new byte[bufSize];
            int bytesRead = inputStream.read(inbuf);
            if (bytesRead < bufSize)
            {
                byte[] inbuf2 = new byte[bytesRead];
                for (int i = 0; i < bytesRead; i++)
                {
                    inbuf2[i] = inbuf[i];
                }
                outstream.writeBinary(inbuf2, bytesRead, adoptOrCopyOnEncode);
            } else
                outstream.writeBinary(inbuf, bytesRead, adoptOrCopyOnEncode);

            long val = 0;
            int val2 = 0;
            float val3 = 0;
            double val4 = 0;
            boolean val5 = false;
            UUID uuid = UUID.randomUUID();
            Date date = new Date();
            
            String str = "John Doe, First St, Cupertino, CA - 95045";          

            if (multipleDataTypes)
            {
                val = rand.nextLong();
                outstream.writeLong(val);

                val2 = rand.nextInt();
                outstream.writeInt(val2);

                outstream.writeUUID(uuid);
                
                outstream.writeUTF8String(str);

                val3 = rand.nextFloat();
                outstream.writeFloat(val3);

                outstream.writeTimeStamp(date);

                val4 = rand.nextDouble();
                outstream.writeDouble(val4);

                val5 = rand.nextBoolean();
                outstream.writeBoolean(val5);
                
                //outstream.writeUTF16String(str2);                
            }

            ChannelBuffer buffer = outstream.getEncodedBuffer();

            CAMQPSyncDecoder inputPipe =
                    CAMQPSyncDecoder
                            .createCAMQPSyncDecoder();
            inputPipe.take(buffer);

            int binaryDataformatCode = inputPipe.readFormatCode();
            assertTrue((binaryDataformatCode == CAMQPFormatCodes.VBIN8)
                    || (binaryDataformatCode == CAMQPFormatCodes.VBIN32));
            long bytesToRead = inputPipe.readBinaryDataSize(binaryDataformatCode);
            assertEquals(bytesRead, bytesToRead);
            while (bytesToRead > 0)
            {
                ChannelBuffer bufferRead =
                        inputPipe.readBinary(binaryDataformatCode, bytesToRead, adoptOrCopyOnDecode);
                bytesToRead -= bufferRead.readableBytes();
            }

            if (multipleDataTypes)
            {
                assertTrue(CAMQPFormatCodes.LONG == inputPipe.readFormatCode());
                assertEquals(val, inputPipe.readLong());
                assertTrue(CAMQPFormatCodes.INT == inputPipe.readFormatCode());
                assertEquals(val2, inputPipe.readInt());
                assertTrue(CAMQPFormatCodes.UUID == inputPipe.readFormatCode());
                assertEquals(uuid, inputPipe.readUUID());
                assertTrue(CAMQPFormatCodes.STR8_UTF8 == inputPipe.readFormatCode());
                String strresult = inputPipe.readString(CAMQPFormatCodes.STR8_UTF8);
                assertEquals(str, strresult);
                assertTrue(CAMQPFormatCodes.FLOAT == inputPipe.readFormatCode());
                assertEquals(val3, inputPipe.readFloat());
                assertTrue(CAMQPFormatCodes.TIMESTAMP == inputPipe.readFormatCode());
                assertEquals(date, inputPipe.readTimeStamp());
                assertTrue(CAMQPFormatCodes.DOUBLE == inputPipe.readFormatCode());
                assertEquals(val4, inputPipe.readDouble());
                if (val5)
                    assertTrue(CAMQPFormatCodes.TRUE == inputPipe
                            .readFormatCode());
                else
                    assertTrue(CAMQPFormatCodes.FALSE == inputPipe
                            .readFormatCode());
                //assertTrue(CAMQPFormatCodes.STR8_UTF16 == inputPipe.readFormatCode());
                //String strresult2 = inputPipe.readString(CAMQPFormatCodes.STR8_UTF16);
                //assertEquals(str2, strresult2);
            }
        }
        inputStream.close();
    }
    
    public void testCodecArray() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        Collection<String> values = new ArrayList<String>();
        values.add("ASCII");
        values.add("EBCDIC");
        
        outstream.writeArrayHeader(2, CAMQPFormatCodes.SYM8);
        for (String s : values)
        {
            outstream.writeUTF8StringArrayElement(s);
        }
        outstream.fillCompoundSize(2);
        
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        int formatCode = inputPipe.readFormatCode();
        assertTrue(CAMQPFormatCodes.ARRAY8 == formatCode);
        long arrayCount = inputPipe.readArrayCount(formatCode);
        assertTrue(arrayCount == 2);
        int arrayElementFormatCode = inputPipe.readFormatCode();
        assertTrue(CAMQPFormatCodes.SYM8 == arrayElementFormatCode);
       
        Collection<String> outValues = new ArrayList<String>();
        for (int i = 0; i < arrayCount; i++)
        {
            String val = inputPipe.readString(arrayElementFormatCode);
            outValues.add(val);
        }
        assertTrue(values.containsAll(outValues));
    }
    
    // TODO
  /*  
    public void testCodecArrayCompoundElements() throws Exception
    {
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        Collection<CAMQPDefinitionExtent> values = new ArrayList<CAMQPDefinitionExtent>();
        
        {
            CAMQPDefinitionExtent extent1 = new CAMQPDefinitionExtent();
            extent1.setFirst(1L);
            extent1.setHandle(350L);
            extent1.setLast(5L);
            extent1.setSettled(true);
            extent1.setRequiredState(false);
            values.add(extent1);
        }

        outstream.writeArrayHeader(1, CAMQPFormatCodes.LIST32);
        for (CAMQPDefinitionExtent s : values)
        {
            CAMQPDefinitionExtent.encodeAsArrayElement(outstream, s);
        }
        outstream.fillCompoundSize(1);
        
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        while (buffer.readable())
        {
            System.out.print(Integer.toHexString(buffer.readByte() & 0xFF) + " ");
        }        
    }    
    */
}
