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

package net.dovemq.transport.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlEnd;

import org.apache.log4j.Logger;

public final class CAMQPSessionFrameHandler
{
    private static final Logger log = Logger.getLogger(CAMQPSessionFrameHandler.class);
    private final ConcurrentMap<Integer, CAMQPSession> sessionsHandshakeInProgress = new ConcurrentHashMap<Integer, CAMQPSession>();

    public static CAMQPSessionFrameHandler createInstance()
    {
        return new CAMQPSessionFrameHandler();
    }

    public void registerSessionHandshakeInProgress(int sendChannelNumber, CAMQPSessionInterface session)
    {
        sessionsHandshakeInProgress.put(sendChannelNumber, (CAMQPSession) session);
    }

    public void frameReceived(int channelNumber, CAMQPFrame frame, CAMQPConnectionInterface connection)
    {
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(frame.getBody());
        String controlName = decoder.readSymbol();
        if (controlName.equalsIgnoreCase(CAMQPControlBegin.descriptor))
        {
            CAMQPControlBegin beginControl = CAMQPControlBegin.decode(decoder);
            int remoteChannelNumber = beginControl.getRemoteChannel();
            if (remoteChannelNumber > 0)
            {
                /*
                 * Session initiator
                 */
                CAMQPSession sessionHIP = sessionsHandshakeInProgress.remove(remoteChannelNumber);
                if (sessionHIP != null)
                {
                    sessionHIP.beginResponse(beginControl, frame.getHeader());
                }
                else
                {
                    // TODO handle error
                    log.error("Could not find session with channelNumber: " + remoteChannelNumber + " in the list of sessions for which handshake is pending");
                }
            }
            else
            {
                /*
                 * session acceptor
                 */
                CAMQPSessionStateActor stateActor = new CAMQPSessionStateActor(connection);
                CAMQPSessionControlWrapper beginContext = new CAMQPSessionControlWrapper(channelNumber, beginControl);
                stateActor.beginReceived(beginContext);
            }
        }
        else if (controlName.equalsIgnoreCase(CAMQPControlEnd.descriptor))
        {
            log.error("No CAMQPSessionHandler found for Session DETACH control");
            log.error("Session DETACH control should have been dispatched directly to CAMQPSessionHandler");
            // TODO handle error
        }
    }
}
