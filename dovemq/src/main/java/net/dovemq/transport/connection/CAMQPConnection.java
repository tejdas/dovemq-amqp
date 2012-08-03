package net.dovemq.transport.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;
import net.dovemq.transport.session.CAMQPSessionFrameHandler;

@ThreadSafe
public class CAMQPConnection
{
    private final CAMQPConnectionStateActor stateActor;

    private CAMQPSender sender = null;

    private final Map<Integer, CAMQPIncomingChannelHandler> incomingChannels = new HashMap<Integer, CAMQPIncomingChannelHandler>();

    private final boolean[] outgoingChannelsInUse = new boolean[CAMQPConnectionConstants.MAX_CHANNELS_SUPPORTED];

    CAMQPConnection(CAMQPConnectionStateActor stateActor)
    {
        Arrays.fill(outgoingChannelsInUse, false);
        this.stateActor = stateActor;
        if (!stateActor.isInitiator)
        {
            sender = stateActor.sender;
            CAMQPConnectionHandler connectionHandler = sender.getChannel().getPipeline().get(CAMQPConnectionHandler.class);
            connectionHandler.registerConnection(this);
        }
    }

    /*
     * For Junit test
     */
    public CAMQPConnection()
    {
        stateActor = null;
    }
    
    public String getRemoteContainerId()
    {
        return stateActor.key.getRemoteContainerId();
    }

    public boolean isInitiator()
    {
        return stateActor.isInitiator;
    }

    public void sendFrame(ChannelBuffer data, int channelId)
    {
        ChannelBuffer header = CAMQPFrameHeader.createEncodedFrameHeader(channelId, data.readableBytes());
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, data), CAMQPFrameConstants.FRAME_TYPE_SESSION);
    }

    void initialize(Channel channel, CAMQPConnectionProperties connectionProps)
    {
        stateActor.setChannel(channel);

        CAMQPConnectionHandler connectionHandler = channel.getPipeline().get(CAMQPConnectionHandler.class);
        connectionHandler.registerConnection(this);

        stateActor.initiateHandshake(connectionProps);
    }

    void waitForReady()
    {
        stateActor.waitForOpenExchange();
        CAMQPConnectionManager.connectionCreated(stateActor.key, this);
        sender = stateActor.sender;
    }

    public void explicitOpen(CAMQPControlOpen openControlData)
    {
        stateActor.sendOpenControl(openControlData);
    }

    public void close()
    {
        stateActor.sendCloseControl();
        sender.waitForClose();
    }

    public void closeAsync()
    {
        stateActor.sendCloseControl();
    }

    public boolean isClosed()
    {
        return sender.isClosed();
    }

    public int reserveOutgoingChannel()
    {
        int maxChannels = stateActor.getConnectionProps().getMaxChannels();
        synchronized (stateActor)
        {
            if (!stateActor.canAttachChannels())
            {
                return -1; // REVISIT TODO
            }
            /*
             * ChannelID 0 is reserved for Connection control
             */
            for (int i = 1; i < maxChannels; i++)
            {
                if (outgoingChannelsInUse[i] == false)
                {
                    outgoingChannelsInUse[i] = true;
                    return i;
                }
            }
        }
        return -1;
    }

    public void register(int receiveChannelNumber, CAMQPIncomingChannelHandler channelHandler)
    {
        synchronized (stateActor)
        {
            if (stateActor.canAttachChannels())
            {
                incomingChannels.put(receiveChannelNumber, channelHandler);
            }
        }
    }

    public void detach(int outgoingChannelNumber, int incomingChannelNumber)
    {
        synchronized (stateActor)
        {
            outgoingChannelsInUse[outgoingChannelNumber] = false;
            incomingChannels.remove(incomingChannelNumber);
        }
    }

    void frameReceived(int channelNumber, CAMQPFrame frame)
    {
        CAMQPIncomingChannelHandler channelHandler = null;
        synchronized (stateActor)
        {
            channelHandler = incomingChannels.get(channelNumber);
        }

        if (channelHandler == null)
        {
            CAMQPSessionFrameHandler.getSingleton().frameReceived(channelNumber, frame, this);
        }
        else
        {
            channelHandler.frameReceived(frame);
        }
    }

    void aborted()
    {
        if (incomingChannels.size() > 0)
        {
            Collection<CAMQPIncomingChannelHandler> channelsToDetach = new ArrayList<CAMQPIncomingChannelHandler>();

            synchronized (stateActor)
            {
                channelsToDetach.addAll(incomingChannels.values());
            }
            for (CAMQPIncomingChannelHandler channelHandler : channelsToDetach)
            {
                channelHandler.channelAbruptlyDetached();
            }
        }
        sender.close();
    }
}
