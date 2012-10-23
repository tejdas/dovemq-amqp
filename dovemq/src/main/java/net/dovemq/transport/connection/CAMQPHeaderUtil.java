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

package net.dovemq.transport.connection;

class CAMQPHeaderUtil
{
    static byte[] composeAMQPHeader()
    {
        byte[] header = new byte[CAMQPConnectionConstants.HEADER_LENGTH];
        header[0] = 'A';
        header[1] = 'M';
        header[2] = 'Q';
        header[3] = 'P';
        header[4] = CAMQPConnectionConstants.PROTOCOL_ID;        
        header[5] = CAMQPConnectionConstants.SUPPORTED_MAJOR_VERSION;
        header[6] = CAMQPConnectionConstants.SUPPORTED_MINOR_VERSION;        
        header[7] = CAMQPConnectionConstants.REVISION;
        return header;
    }

    static boolean validateAMQPHeader(byte[] amqpHeader)
    {
        return ((amqpHeader.length == CAMQPConnectionConstants.HEADER_LENGTH)
                && (amqpHeader[0] == 'A')
                && (amqpHeader[1] == 'M')
                && (amqpHeader[2] == 'Q')
                && (amqpHeader[3] == 'P')
                && (amqpHeader[4] == CAMQPConnectionConstants.PROTOCOL_ID)                
                && (amqpHeader[5] <= CAMQPConnectionConstants.SUPPORTED_MAJOR_VERSION)
                && (amqpHeader[6] <= CAMQPConnectionConstants.SUPPORTED_MINOR_VERSION)
                && (amqpHeader[7] <= CAMQPConnectionConstants.REVISION));
    }
}

