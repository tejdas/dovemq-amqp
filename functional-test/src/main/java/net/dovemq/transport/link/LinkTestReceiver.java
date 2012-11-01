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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;

public class LinkTestReceiver
{
    public static void main(String[] args) throws InterruptedException, IOException, MalformedObjectNameException
    {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];

        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);

        String source = args[3];
        String target = args[4];

        String brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);

        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();

        CAMQPLinkReceiverInterface linkReceiver = CAMQPLinkFactory.createLinkReceiver(brokerContainerId, source, target, new CAMQPEndpointPolicy());
        System.out.println("Receiver Link created between : " + source + "  and: " + target);
        long expectedMessageCount = 500;

        mbeanProxy.registerSource(source, target, expectedMessageCount);

        LinkTestTarget linktarget = new LinkTestTarget();
        linkReceiver.registerTarget(linktarget);

        long requestedMessageCount = 0;

        Random randomGenerator = new Random();
        while (linktarget.getNumberOfMessagesReceived() < expectedMessageCount)
        {
            int randomInt = randomGenerator.nextInt(50) + 5;
            long messagesYetToBeReceived = expectedMessageCount - linktarget.getNumberOfMessagesReceived();

            if (randomInt > messagesYetToBeReceived)
            {
                requestedMessageCount = expectedMessageCount;
                System.out.println("Requesting extra number of messages: "  + (randomInt - messagesYetToBeReceived));
            }
            else
            {
                requestedMessageCount += randomInt;
            }

            linkReceiver.getMessages(randomInt);

            while (linktarget.getNumberOfMessagesReceived() < requestedMessageCount)
                Thread.sleep(500);

            System.out.println("received " + requestedMessageCount + " messages so far");
        }

        assertTrue(linktarget.getNumberOfMessagesReceived() == expectedMessageCount);

        ((CAMQPLinkReceiver)linkReceiver).destroyLink();

        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();

        jmxWrapper.cleanup();
    }
}
