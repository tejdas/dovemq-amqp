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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MalformedObjectNameException;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.common.JMXProxyWrapper;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.EndpointTestUtils;
import net.dovemq.transport.session.SessionCommand;

public class LinkTestMultipleSources {
    private static final String source = "src";

    private static final String target = "target";

    private static String brokerContainerId;

    private static int NUM_THREADS = 5;

    private static LinkCommandMBean mbeanProxy;

    private static final CountDownLatch startSignal = new CountDownLatch(1);

    private static volatile CountDownLatch doneSignal = null;

    private static class LinkSourceDriver implements Runnable {
        public LinkSourceDriver(int numMessagesToSend) {
            super();
            this.numMessagesToSend = numMessagesToSend;
        }

        private final int numMessagesToSend;

        @Override
        public void run() {
            try {
                startSignal.await();
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
            String localSource = String.format("%s%d",
                    source,
                    Thread.currentThread().getId());
            String localTarget = String.format("%s%d",
                    target,
                    Thread.currentThread().getId());
            CAMQPSourceInterface sender = CAMQPEndpointManager.createSource(brokerContainerId,
                    localSource,
                    localTarget,
                    new CAMQPEndpointPolicy());
            mbeanProxy.attachSharedTarget(localSource, localTarget);

            Random randomGenerator = new Random();

            /*
             * Since the functional test uses NUM_THREADS=5 and
             * numMessagesToSend=50000, the test fails on Windows because of
             * repeated calls to RandomStringUtils.randomAlphanumeric().
             *
             * The following boolean is to suppress call to RandomStringUtils.
             */
            boolean generateRandomString = false;
            for (int i = 0; i < numMessagesToSend; i++) {
                DoveMQMessage message = EndpointTestUtils.createSmallEncodedMessage(randomGenerator,
                        generateRandomString);
                sender.sendMessage(message);
            }
            doneSignal.countDown();
        }
    }

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

        NUM_THREADS = Integer.parseInt(args[3]);
        int numMessagesToSend = Integer.parseInt(args[4]);

        doneSignal = new CountDownLatch(NUM_THREADS);

        brokerContainerId = String.format("broker@%s", brokerIp);
        CAMQPLinkManager.initialize(false, publisherName);

        SessionCommand localSessionCommand = new SessionCommand();
        localSessionCommand.sessionCreate(brokerContainerId);

        mbeanProxy = jmxWrapper.getLinkBean();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int numMessagesExpected = numMessagesToSend * NUM_THREADS;

        LinkSourceDriver[] senders = new LinkSourceDriver[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            LinkSourceDriver sender = new LinkSourceDriver(numMessagesToSend);
            senders[i] = sender;
            executor.submit(sender);
        }

        startSignal.countDown();

        doneSignal.await();
        Thread.sleep(2000);

        while (true) {
            Thread.sleep(1000);
            long numMessagesReceivedAtRemote = mbeanProxy.getNumMessagesReceivedAtTargetReceiver();
            System.out.println(numMessagesReceivedAtRemote);
            if (numMessagesReceivedAtRemote == numMessagesExpected)
                break;
        }

        System.out.println("Got all messages: sleeping for 10 secs");
        Thread.sleep(10000);
        executor.shutdown();
        CAMQPLinkManager.shutdown();
        System.out.println("Shutdown linkManager");
        mbeanProxy.reset();
        jmxWrapper.cleanup();
        System.out.println("cleanup JMX");
    }
}
