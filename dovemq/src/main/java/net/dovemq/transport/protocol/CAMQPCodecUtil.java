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

package net.dovemq.transport.protocol;

import java.util.Date;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;

public final class CAMQPCodecUtil {
    private static final String HEXES = "0123456789ABCDEF";

    public static String getHex(ChannelBuffer buffer) {
        byte[] raw = buffer.array();
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(3 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)))
                    .append(' ');
        }
        return hex.toString();
    }

    public static int computeWidth(int formatCode) {
        int formatCodeShift = formatCode >> 4;
        if ((formatCode >= 0x40) && (formatCode <= 0x9F)) {
            if (formatCodeShift == 0x04)
                return 0;
            else
                return 0x01 << ((formatCodeShift & 0x0F) - 5);
        }
        return ((formatCodeShift & 0x01) == 1) ? 4 : 1;
    }

    public static void writeUByte(int value, ChannelBuffer writableBuffer) {
        byte unsignedVal = (byte) (value & 0xFF);
        writableBuffer.writeByte(unsignedVal);
    }

    public static void writeUByteAt(int value, int position, ChannelBuffer writableBuffer) {
        byte unsignedVal = (byte) (value & 0xFF);
        writableBuffer.setByte(position, unsignedVal);
    }

    public static void writeUShort(int value, ChannelBuffer writableBuffer) {
        final byte[] unsignedVal = new byte[] { (byte) ((value >> CAMQPProtocolConstants.OCTET) & 0xFF), (byte) (value & 0xFF) };
        writableBuffer.writeBytes(unsignedVal);
    }

    public static void writeUInt(long value, ChannelBuffer writableBuffer) {
        int width = 4;
        byte[] uIntVal = new byte[width];
        for (int i = 0; i < width; i++) {
            uIntVal[i] = (byte) ((value >> (CAMQPProtocolConstants.OCTET * ((width - 1) - i))) & 0xFF);
        }
        writableBuffer.writeBytes(uIntVal);
    }

    public static void writeUIntAt(long value, int position, ChannelBuffer writableBuffer) {
        int width = 4;
        byte[] uIntVal = new byte[width];
        for (int i = 0; i < width; i++) {
            uIntVal[i] = (byte) ((value >> (CAMQPProtocolConstants.OCTET * ((width - 1) - i))) & 0xFF);
        }
        writableBuffer.setBytes(position, uIntVal);
    }

    static int readFormatCode(ChannelBuffer buffer) {
        byte formatCodeByte = buffer.readByte();
        return (formatCodeByte & 0xFF);
    }

    public static int readUByte(ChannelBuffer buffer) {
        byte val = buffer.readByte();
        return (val & 0xFF);
    }

    public static int readUShort(ChannelBuffer buffer) {
        byte[] val = new byte[2];
        buffer.readBytes(val);
        return ((val[0] & 0xFF) << CAMQPProtocolConstants.OCTET) + (val[1] & 0xFF);
    }

    public static long readUInt(ChannelBuffer buffer) {
        int width = 4;
        byte[] val = new byte[width];
        buffer.readBytes(val);
        return ((long) (val[0] & 0xFF) << 24) + ((val[1] & 0xFF) << 16) + ((val[2] & 0xFF) << 8) + (val[3] & 0xFF);
    }

    static byte readByte(ChannelBuffer buffer) {
        return buffer.readByte();
    }

    static short readShort(ChannelBuffer buffer) {
        return buffer.readShort();
    }

    static int readInt(ChannelBuffer buffer) {
        return buffer.readInt();
    }

    static long readLong(ChannelBuffer buffer) {
        return buffer.readLong();
    }

    static float readFloat(ChannelBuffer buffer) {
        int intVal = buffer.readInt();
        return Float.intBitsToFloat(intVal);
    }

    static double readDouble(ChannelBuffer buffer) {
        long longVal = buffer.readLong();
        return Double.longBitsToDouble(longVal);
    }

    static Date readTimeStamp(ChannelBuffer buffer) {
        long longVal = buffer.readLong();
        return new Date(longVal);
    }

    static UUID readUUID(ChannelBuffer buffer) {
        long msb = buffer.readLong();
        long lsb = buffer.readLong();
        return new UUID(msb, lsb);
    }
}
