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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;

public class LinkTestMultipleSendersSharingOneDelayedLink
{
    private static final String source = "src";

    private static final String target = "target";

    private static int NUM_THREADS = 10;

    private static class LinkTestMessageSender extends CAMQPTestTask implements
            Runnable
    {
        private final CAMQPLinkSender linkSender;

        private final int numMessagesToSend;

        public LinkTestMessageSender(CountDownLatch startSignal,
                CountDownLatch doneSignal,
                CAMQPLinkSender linkSender,
                int numMessagesToSend)
        {
            super(startSignal, doneSignal);
            this.linkSender = linkSender;
            this.numMessagesToSend = numMessagesToSend;
        }

        @Override
        public void run()
        {
            waitForReady();
            LinkTestUtils.sendMessagesOnLink(linkSender, numMessagesToSend);
            done();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, MalformedObjectNameException
    {
        /*
         * Read args
         */
        String publisherName = args[0];
        String brokerIp = args[1];
        String jmxPort = args[2];

        NUM_THREADS = Integer.parseInt(args[3]);
        int numMessagesToSend = Integer.parseInt(args[4]);

        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(brokerIp, jmxPort);

        String brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);

        LinkCommandMBean mbeanProxy = jmxWrapper.getLinkBean();

        CAMQPLinkSender linkSender = (CAMQPLinkSender) CAMQPLinkFactory.createLinkSender(brokerContainerId, source, target, new CAMQPEndpointPolicy());
        System.out.println("Sender Link created between : " + source + "  and: " + target);

        mbeanProxy.registerDelayedTarget(source, target, 500);
        /*
         * ReceiverLinkCreditPolicy.
         * CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING
         */
        String linkName = linkSender.getLinkName();
        mbeanProxy.setLinkCreditSteadyState(linkName, 100, 500, ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);

        Thread.sleep(2000);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++)
        {
            LinkTestMessageSender sender = new LinkTestMessageSender(startSignal, doneSignal, linkSender, numMessagesToSend);
            executor.submit(sender);
        }

        startSignal.countDown();
        while (true)
        {
            long messagesReceived = mbeanProxy.getNumMessagesProcessedByDelayedEndpoint();
            System.out.println("got messages: " + messagesReceived);
            if (messagesReceived == numMessagesToSend * NUM_THREADS)
            {
                break;
            }
            Thread.sleep(1000);
        }

        assertTrue(mbeanProxy.getNumMessagesProcessedByDelayedEndpoint() == numMessagesToSend * NUM_THREADS);

        while (!mbeanProxy.processedAllMessages())
        {
            System.out.println("still processing messages");
            Thread.sleep(1000);
        }

        doneSignal.await();
        Thread.sleep(2000);
        executor.shutdown();

        linkSender.destroyLink();

        CAMQPLinkManager.shutdown();
        mbeanProxy.reset();
        jmxWrapper.cleanup();
    }
}
