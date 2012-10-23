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

import java.util.Collection;

public class ConnectionCommand implements ConnectionCommandMBean
{
    private volatile boolean shutdown = false;
    public boolean isShutdown()
    {
        return shutdown;
    }

    @Override
    public void help()
    {
        System.out.println("shutdown");
        System.out.println("list");
        System.out.println("create [targetContainerId]");
        System.out.println("close [targetContainerId]");
        System.out.println("closeAsync [targetContainerId]");
        System.out.println("isClosed [targetContainerId]");
    }

    @Override
    public void create(String targetContainerId)
    {
        CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
        CAMQPConnection connection = CAMQPConnectionFactory.createCAMQPConnection(targetContainerId, connectionProps);
        if (connection == null)
            System.out.println("AMQP connection could not be created");
    }

    @Override
    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public Collection<String> list()
    {
        Collection<String> connectionList = CAMQPConnectionManager.listConnections();
        if (connectionList.size() > 0)
        {
            System.out.println("List of connections by targetContainerId");
            for (String conn : connectionList)
            {
                System.out.println(conn);
            }
        }
        return connectionList;
    }

    @Override
    public void close(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        conn.close();
    }

    @Override
    public boolean checkClosed(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        if ((conn == null) || conn.isClosed())
            return true;
        else
            return false;
    }

    @Override
    public void closeAsync(String targetContainerId)
    {
        CAMQPConnection conn = CAMQPConnectionManager.getCAMQPConnection(targetContainerId);
        conn.closeAsync();
    }
}
