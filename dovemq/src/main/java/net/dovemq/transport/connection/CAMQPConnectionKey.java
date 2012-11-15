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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class CAMQPConnectionKey
{
    public CAMQPConnectionKey(String remoteContainerId, int ephemeralPort)
    {
        super();
        this.remoteContainerId = remoteContainerId;
        this.ephemeralPort = ephemeralPort;
    }

    public String getRemoteContainerId()
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
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return remoteContainerId + "." + ephemeralPort;
    }
}
