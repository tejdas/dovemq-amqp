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

package net.dovemq.transport.link;

import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.EndpointTestUtils;

public class LinkTestBiDir {
    private static final String source = "src";

    private static final String target = "target";

    private static String brokerContainerId;

    private static LinkCommandMBean mbeanProxy;

    public static void main(String[] args) throws InterruptedException,
            IOException,
            MalformedObjectNameException {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];

        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);

        int messagesToSend = Integer.parseInt(args[3]);

        brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);

        String containerId = CAMQPConnectionManager.getContainerId();

        mbeanProxy = jmxWrapper.getLinkBean();

        CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId,
                source,
                target,
                new CAMQPEndpointPolicy());
        mbeanProxy.attachTarget(source, target);

        mbeanProxy.createSource("reverseSrc", "reverseTar", containerId);
        LinkCommand localLinkCommand = new LinkCommand();
        localLinkCommand.attachTarget("reverseSrc", "reverseTar");

        Random randomGenerator = new Random();
        for (int i = 0; i < messagesToSend; i++) {
            DoveMQMessage message = EndpointTestUtils.createEncodedMessage(randomGenerator,
                    true);
            sender.sendMessage(message);
        }
        System.out.println("Done sending messages");

        while (true) {
            Thread.sleep(1000);
            long numMessagesReceivedAtRemote = mbeanProxy.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtRemote);
            long numMessagesReceivedAtLocal = localLinkCommand.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtLocal);
            if ((numMessagesReceivedAtRemote == messagesToSend) && (numMessagesReceivedAtLocal == messagesToSend))
                break;
        }

        System.out.println("Done: sleeping for 5 seconds");
        Thread.sleep(5000);
        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
