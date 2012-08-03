package net.dovemq.transport.session;

import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlEnd;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

public class CAMQPSessionFrameHandler
{
    private static final Logger log = Logger.getLogger(CAMQPSessionFrameHandler.class);

    private static final CAMQPSessionFrameHandler sessionHandler = new CAMQPSessionFrameHandler();

    private final ConcurrentMap<Integer, CAMQPSession> sessionsHandshakeInProgress = new ConcurrentHashMap<Integer, CAMQPSession>();

    public static CAMQPSessionFrameHandler getSingleton()
    {
        return sessionHandler;
    }

    void registerSessionHandshakeInProgress(int sendChannelNumber, CAMQPSession session)
    {
        sessionsHandshakeInProgress.put(sendChannelNumber, session);
    }

    public void frameReceived(int channelNumber, CAMQPFrame frame, CAMQPConnection connection)
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
            // REVISIT TODO handle error
        }
    }
}
