package net.dovemq.transport.connection;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

class CAMQPConnectionKey
{
    private static final String BROKER_ID = "broker";
    CAMQPConnectionKey(String remoteContainerId, int ephemeralPort)
    {
        super();
        this.remoteContainerId = remoteContainerId;
        this.ephemeralPort = ephemeralPort;
    }

    String getRemoteContainerId()
    {
        return remoteContainerId;
    }

    void setRemoteContainerId(String remoteContainerId)
    {
        this.remoteContainerId = remoteContainerId;
    }

    int getEphemeralPort()
    {
        return ephemeralPort;
    }

    void setEphemeralPort(int ephemeralPort)
    {
        this.ephemeralPort = ephemeralPort;
    }

    CAMQPConnectionKey()
    {
    }

    private String remoteContainerId;
    private int ephemeralPort = 0;

    @Override
    public boolean equals(Object obj)
    {
        if ((obj == null) || (!(obj instanceof CAMQPConnectionKey)))
            return false;       
        
        CAMQPConnectionKey otherKey = (CAMQPConnectionKey) obj;
        
        if ((ephemeralPort == 0) || (otherKey.ephemeralPort == 0))
            return (remoteContainerId.equalsIgnoreCase(otherKey.remoteContainerId));

        return EqualsBuilder.reflectionEquals(this, obj);
    }
    
    @Override
    public int hashCode()
    {
        if (remoteContainerId.contains(BROKER_ID))
            return remoteContainerId.hashCode();
        return HashCodeBuilder.reflectionHashCode(this);
    }
    
    @Override
    public String toString()
    {
        return remoteContainerId + "." + ephemeralPort;
    }
}
