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

public class CAMQPProtocolConstants
{
    public static final int OCTET = 8;
    protected static final int UBYTE_MAX_VALUE = 255;
    protected static final int USHORT_MAX_VALUE = 65535;
    protected static final long UINT_MAX_VALUE = 4294967295L;
    public static final int INT_MAX_VALUE =   0x7FFFFFFF; // 2147483647;

    protected static final int DYNAMIC_BUFFER_INITIAL_SIZE = 4096;

    protected static final int DEFAULT_MAX_CHANNELS = 16;
    protected static final int DEFAULT_HEARTBEAT_INTERVAL = 1024;

    public static final String CHARSET_UTF8 = "UTF-8";
    protected static final String CHARSET_UTF16 = "UTF-16";

    public static final String SYMBOL_BINARY_PAYLOAD = "amqp:data:binary";
}
