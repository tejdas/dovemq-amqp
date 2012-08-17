package net.dovemq.transport.connection;

import net.dovemq.transport.protocol.CAMQPProtocolConstants;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;

public final class CAMQPConnectionProperties
{
    public static CAMQPConnectionProperties createConnectionProperties()
    {
        return new CAMQPConnectionProperties();
    }

    private CAMQPConnectionProperties()
    {
    }

    CAMQPConnectionProperties cloneProperties()
    {
        CAMQPConnectionProperties cloned = new CAMQPConnectionProperties();
        cloned.heartbeatInterval = this.heartbeatInterval;
        cloned.maxChannels = this.maxChannels;
        cloned.maxFrameSizeSupported = this.maxFrameSizeSupported;
        return cloned;
    }

    void update(CAMQPControlOpen peerRequested)
    {
        if (peerRequested.getChannelMax() <= CAMQPConnectionConstants.MAX_CHANNELS_SUPPORTED)
        {
            this.maxChannels = peerRequested.getChannelMax();
        }
        else
        {
            this.maxChannels = CAMQPConnectionConstants.MAX_CHANNELS_SUPPORTED;
        }
        if (peerRequested.getMaxFrameSize() <= CAMQPProtocolConstants.INT_MAX_VALUE)
        {
            this.maxFrameSizeSupported = peerRequested.getMaxFrameSize();
        }
        else
        {
            this.maxFrameSizeSupported = CAMQPProtocolConstants.INT_MAX_VALUE;
        }
    }

    long getMaxFrameSizeSupported()
    {
        return maxFrameSizeSupported;
    }

    void setMaxFrameSizeSupported(long maxFrameSizeSupported)
    {
        this.maxFrameSizeSupported = maxFrameSizeSupported;
    }

    int getMaxChannels()
    {
        return maxChannels;
    }

    void setMaxChannels(int maxChannels)
    {
        this.maxChannels = maxChannels;
    }

    long getHeartbeatInterval()
    {
        return heartbeatInterval;
    }

    void setHeartbeatInterval(long heartbeatInterval)
    {
        this.heartbeatInterval = heartbeatInterval;
    }

    private long maxFrameSizeSupported = CAMQPProtocolConstants.INT_MAX_VALUE;

    private int maxChannels = CAMQPConnectionConstants.MAX_CHANNELS_SUPPORTED;

    private long heartbeatInterval = CAMQPConnectionConstants.HEARTBEAT_PERIOD;
}
