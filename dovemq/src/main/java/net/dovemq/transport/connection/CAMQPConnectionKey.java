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

import java.net.InetSocketAddress;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jboss.netty.channel.Channel;

public final class CAMQPConnectionKey {
    public String getRemoteContainerId() {
        return remoteContainerId;
    }

    void setRemoteContainerId(String remoteContainerId) {
        this.remoteContainerId = remoteContainerId;
    }

    void setAddress(Channel channel) {
        this.remoteHostAddress = ((InetSocketAddress) channel.getRemoteAddress()).getHostString();
        this.remotePort = ((InetSocketAddress) channel.getRemoteAddress()).getPort();
        this.localHostAddress = ((InetSocketAddress) channel.getLocalAddress()).getHostString();
        this.localPort = ((InetSocketAddress) channel.getLocalAddress()).getPort();
    }

    private String remoteContainerId;
    private String remoteHostAddress;
    private int remotePort = 0;
    private String localHostAddress;
    private int localPort = 0;

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return String.format("%s@%s:%d-%s:%d", remoteContainerId,
                remoteHostAddress, remotePort, localHostAddress, localPort);
    }
}
