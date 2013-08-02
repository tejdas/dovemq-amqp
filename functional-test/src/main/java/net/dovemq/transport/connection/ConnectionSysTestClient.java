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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

public class ConnectionSysTestClient {
    public static void main(String[] args) throws IOException,
            InterruptedException {
        Random r = new Random();
        Thread.sleep(500 + r.nextInt(500));
        String id = (args.length == 0) ? "publisher" : args[0];
        CAMQPConnectionManager.initialize(id);
        System.out.println("AMQP client container ID: " + CAMQPConnectionManager.getContainerId());

        String brokerContainerId = String.format("broker@%s", args[1]);
        ConnectionCommandMBean commandExecutor = new ConnectionCommand();
        commandExecutor.create(brokerContainerId);
        Thread.sleep(500 + r.nextInt(500));

        System.out.println("Connection created");

        Collection<String> connectionList = CAMQPConnectionManager.listConnections();
        assertTrue(connectionList.size() == 1);
        for (String s : connectionList) {
            assertTrue(s.contains(brokerContainerId));
        }

        Thread.sleep(500 + r.nextInt(500));

        commandExecutor.close(brokerContainerId);

        Thread.sleep(500 + r.nextInt(500));

        assertTrue(commandExecutor.checkClosed(brokerContainerId));

        System.out.println("Connection closed");

        Thread.sleep(500 + r.nextInt(500));

        Collection<String> connectionListAfterClose = CAMQPConnectionManager.listConnections();
        assertTrue(!connectionListAfterClose.contains(brokerContainerId));

        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
    }
}
