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

package net.dovemq.transport.protocol.data;

public class CAMQPFormatCodes
{
    public static final int VBIN8 = 0xa0;
    public static final int VBIN32 = 0xb0;
    public static final int SYM8 = 0xa3;
    public static final int SYM32 = 0xb3;
    public static final int CHAR = 0x73;
    public static final int INT = 0x71;
    public static final int SMALLINT = 0x54;
    public static final int LIST8 = 0xc0;
    public static final int LIST32 = 0xd0;
    public static final int DECIMAL64 = 0x84;
    public static final int USHORT = 0x60;
    public static final int LONG = 0x81;
    public static final int SMALLLONG = 0x55;
    public static final int DOUBLE = 0x82;
    public static final int FLOAT = 0x72;
    public static final int TIMESTAMP = 0x83;
    public static final int SHORT = 0x61;
    public static final int BYTE = 0x51;
    public static final int STR8_UTF8 = 0xa1;
    public static final int STR32_UTF8 = 0xb1;
    public static final int MAP8 = 0xc1;
    public static final int MAP32 = 0xd1;
    public static final int BOOLEAN = 0x56;
    public static final int TRUE = 0x41;
    public static final int FALSE = 0x42;
    public static final int UUID = 0x98;
    public static final int UBYTE = 0x50;
    public static final int ULONG = 0x80;
    public static final int SMALLULONG = 0x53;
    public static final int ULONG0 = 0x44;
    public static final int DECIMAL128 = 0x94;
    public static final int NULL = 0x40;
    public static final int UINT = 0x70;
    public static final int SMALLUINT = 0x52;
    public static final int UINT0 = 0x43;
    public static final int ARRAY8 = 0xe0;
    public static final int ARRAY32 = 0xf0;
    public static final int DECIMAL32 = 0x74;
}
