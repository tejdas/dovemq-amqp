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

import java.io.IOException;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;

public class SessionSysTestServer
{
    public static void main(String[] args) throws InterruptedException, IOException, CAMQPSessionBeginException
    {
        CAMQPConnectionManager.initialize("broker");
        System.out.println("container ID: " + CAMQPConnectionManager.getContainerId());
        CAMQPConnectionProperties defaultConnectionProps =
            CAMQPConnectionProperties.createConnectionProperties();

        CAMQPListener listener =
                CAMQPListener.createCAMQPListener(defaultConnectionProps);
        listener.start();

        SessionSysTestCmdDriver.processConsoleInput(true);

        CAMQPSessionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        listener.shutdown();
    }
}
