package net.dovemq.transport.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;
import net.dovemq.transport.session.CAMQPSessionFrameHandler;

/**
 * AMQP Connection implementation on the outgoing side
 * 
 *    ==>> CAMQPConnection ==>>
 * <<== CAMQPConnectionHandler <<==
 * @author tejdas
 *
 */
@ThreadSafe
public class CAMQPConnection
{
    private final CAMQPConnectionStateActor stateActor;

    private CAMQPSender sender = null;

    @GuardedBy("stateActor")
    private final Map<Integer, CAMQPIncomingChannelHandler> incomingChannels = new HashMap<Integer, CAMQPIncomingChannelHandler>();
    @GuardedBy("stateActor")
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

    /**
     * Send an AMQP frame on the specified channel
     * @param data
     * @param channelId
     */
    public void sendFrame(ChannelBuffer data, int channelId)
    {
        ChannelBuffer header = CAMQPFrameHeader.createEncodedFrameHeader(channelId, data.readableBytes());
        sender.sendBuffer(ChannelBuffers.wrappedBuffer(header, data), CAMQPFrameConstants.FRAME_TYPE_SESSION);
    }

    /**
     * Initiate AMQP connection handshake
     * @param channel
     * @param connectionProps
     */
    void initialize(Channel channel, CAMQPConnectionProperties connectionProps)
    {
        stateActor.setChannel(channel);

        CAMQPConnectionHandler connectionHandler = channel.getPipeline().get(CAMQPConnectionHandler.class);
        connectionHandler.registerConnection(this);

        stateActor.initiateHandshake(connectionProps);
    }

    /**
     * Wait for AMQP handshake to complete
     */
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

    /**
     * Synchronously close the connection
     */
    public void close()
    {
        stateActor.sendCloseControl();
        sender.waitForClose();
    }

    /**
     * Asynchronously close the connection
     */
    public void closeAsync()
    {
        stateActor.sendCloseControl();
    }

    public boolean isClosed()
    {
        return sender.isClosed();
    }

    /**
     * Called by the session layer to reserve
     * an outgoing channel, which it will
     * subsequently attach to.
     * @return
     */
    public int reserveOutgoingChannel()
    {
        synchronized (stateActor)
        {
            int maxChannels = stateActor.getConnectionProps().getMaxChannels();
            if (!stateActor.canAttachChannels())
            {
                return -1; // TODO
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

    /**
     * Register an incoming channel with the channelHandler, so that
     * the incoming frames on the rxChannel could be dispatched.
     * 
     * @param receiveChannelNumber
     * @param channelHandler
     */
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

    /**
     * Called by the session layer to detach itself from an
     * outgoing channel.
     * 
     * @param outgoingChannelNumber
     * @param incomingChannelNumber
     */
    public void detach(int outgoingChannelNumber, int incomingChannelNumber)
    {
        synchronized (stateActor)
        {
            outgoingChannelsInUse[outgoingChannelNumber] = false;
            incomingChannels.remove(incomingChannelNumber);
        }
    }

    /**
     * Dispatches the incoming AMQP frame to the channelHandler
     * associated with the channelNumber (if it's already attached),
     * or to CAMQPSessionFrameHandler if it hasn't been attached yet.
     * 
     * @param channelNumber
     * @param frame
     */
    void frameReceived(int channelNumber, CAMQPFrame frame)
    {
        CAMQPIncomingChannelHandler channelHandler = null;
        synchronized (stateActor)
        {
            channelHandler = incomingChannels.get(channelNumber);
        }

        if (channelHandler == null)
        {
            /*
             * The channel has not been attached yet. Dispatch to the
             * CAMQPSessionFrameHandler so it can attach a channelHandler
             * to it.
             */
            CAMQPSessionFrameHandler.getSingleton().frameReceived(channelNumber, frame, this);
        }
        else
        {
            channelHandler.frameReceived(frame);
        }
    }

    void aborted()
    {
        Collection<CAMQPIncomingChannelHandler> channelsToDetach = new ArrayList<CAMQPIncomingChannelHandler>();

        synchronized (stateActor)
        {
            if (incomingChannels.size() > 0)
                channelsToDetach.addAll(incomingChannels.values());
        }
        for (CAMQPIncomingChannelHandler channelHandler : channelsToDetach)
        {
            channelHandler.channelAbruptlyDetached();
        }
        sender.close();
    }
}
