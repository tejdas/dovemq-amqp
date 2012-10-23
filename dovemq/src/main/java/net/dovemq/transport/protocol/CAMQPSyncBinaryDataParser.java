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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

class CAMQPSyncBinaryDataParser
{
    static ChannelBuffer parseBinaryData(ChannelBuffer buffer, long size, boolean copyFree)
    {
        if (!buffer.readable())
        {
            return null;
        }

        int toRead = (size > (CAMQPProtocolConstants.USHORT_MAX_VALUE + 1)) ? (CAMQPProtocolConstants.USHORT_MAX_VALUE + 1) : (int) size;
        int readableBytes = buffer.readableBytes();
        if (toRead <= readableBytes)
        {
            return readBuffer(buffer, toRead, copyFree);
        }
        else
        {
            return readBuffer(buffer, readableBytes, copyFree);
        }
    }

    private static ChannelBuffer readBuffer(ChannelBuffer buffer, int bytesToRead, boolean copyFree)
    {
        ChannelBuffer binaryData;
        if (copyFree)
        {
            int readerIndex = buffer.readerIndex();
            binaryData = buffer.slice(readerIndex, bytesToRead);
            buffer.readerIndex(readerIndex + bytesToRead);
        }
        else
        {
            binaryData = ChannelBuffers.buffer(CAMQPProtocolConstants.USHORT_MAX_VALUE + 1);
            buffer.readBytes(binaryData, bytesToRead);
            buffer.discardReadBytes();
        }
        return binaryData;
    }
}
