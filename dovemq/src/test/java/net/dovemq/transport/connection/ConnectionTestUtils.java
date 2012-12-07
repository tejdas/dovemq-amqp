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

import java.util.concurrent.BlockingQueue;

import net.dovemq.transport.session.CAMQPSessionInterface;

import org.jboss.netty.buffer.ChannelBuffer;

public class ConnectionTestUtils
{
    public static CAMQPConnectionInterface createStubConnection(final BlockingQueue<ChannelBuffer> framesQueue)
    {
        return new CAMQPConnectionInterface() {
            @Override
            public void sendFrame(ChannelBuffer buffer, int channelId)
            {
                framesQueue.add(buffer);
            }

            @Override
            public int reserveOutgoingChannel()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void register(int receiveChannelNumber,
                    CAMQPIncomingChannelHandler channelHandler)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void registerSessionHandshakeInProgress(int sendChannelNumber,
                    CAMQPSessionInterface session)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void detach(int outgoingChannelNumber,
                    int incomingChannelNumber)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public CAMQPConnectionKey getKey()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void close()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void closeAsync()
            {
                // TODO Auto-generated method stub

            }
        };
    }
}
