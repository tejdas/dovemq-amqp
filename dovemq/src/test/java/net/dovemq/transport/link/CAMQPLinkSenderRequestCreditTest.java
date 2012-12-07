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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.connection.ConnectionTestUtils;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;
import net.dovemq.transport.session.CAMQPSessionSenderTest;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CAMQPLinkSenderRequestCreditTest
{
    private Mockery mockContext = null;
    private CAMQPSessionInterface session = null;
    private CAMQPConnectionInterface mockConnection = null;

    public CAMQPLinkSender linkSender = null;
    private long linkHandle = 1;

    private final BlockingQueue<ChannelBuffer> framesQueue = new LinkedBlockingQueue<ChannelBuffer>();
    private final CAMQPLinkSendFlowScheduler flowScheduler = new CAMQPLinkSendFlowScheduler();

    @BeforeClass
    public static void setupBeforeClass()
    {
        CAMQPSessionManager.initialize();
    }

    @Before
    public void setup()
    {
        mockContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        mockConnection =  ConnectionTestUtils.createStubConnection(framesQueue);
        session = CAMQPSessionSenderTest.createMockSessionAndSetExpectations(mockContext, mockConnection);
        flowScheduler.start();

        linkSender = new CAMQPLinkSender(session) {
            @Override
            void registerWithFlowScheduler()
            {
                flowScheduler.registerLinkSender(1, this);
            }

            @Override
            void unregisterWithFlowScheduler()
            {
                flowScheduler.unregisterLinkSender(1);
            }
        };
        linkSender.setMaxAvailableLimit(4096);
    }

    @After
    public void tearDown()
    {
        flowScheduler.stop();
        mockContext.assertIsSatisfied();
    }

    @AfterClass
    public static void teardownAfterClass()
    {
        CAMQPSessionManager.shutdown();
    }

    @Test
    public void testLinkSenderRequestsCredit() throws InterruptedException
    {
        linkSender.registerWithFlowScheduler();

        linkSender.setMaxAvailableLimit(4096);

        /*
         * Send a flow-frame to Link Sender and make it's
         * Link credit 0.
         */
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);

        flow.setDrain(false);
        flow.setEcho(false);
        flow.setLinkCredit(0L);
        flow.setIncomingWindow(20L);
        flow.setNextIncomingId(0L);
        flow.setDeliveryCount(0L);
        linkSender.flowReceived(flow);

        /*
         * Send a message on the Link Sender.
         * This should trigger the Link Sender
         * to request for link credit.
         */
        Random r = new Random();
        String deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(new CAMQPMessage(deliveryTag, CAMQPLinkSenderTest.createMessage(r)));

        /*
         * Wait for 35 seconds, and assert that the LinkSender generates
         * 4 flow-frames (at intervals 2, 6, 16, 30)
         */
        getAndAssertAMQPFrames(4, CAMQPControlFlow.descriptor, 35000);

        /*
         * Give a link-credit of 1, and assert that the LinkSender
         * sends the messages.
         */
        flow.setLinkCredit(1L);
        linkSender.flowReceived(flow);
        getAndAssertAMQPFrames(1, CAMQPControlTransfer.descriptor, 5000);

        /*
         * Send another message on the Link Sender.
         * This should trigger the Link Sender
         * to request for link credit.
         */
        deliveryTag = UUID.randomUUID().toString();
        linkSender.sendMessage(new CAMQPMessage(deliveryTag, CAMQPLinkSenderTest.createMessage(r)));

        /*
         * Wait for 35 seconds, and assert that the LinkSender generates
         * 4 flow-frames (at intervals 2, 6, 16, 30)
         */
        getAndAssertAMQPFrames(4, CAMQPControlFlow.descriptor, 35000);

        linkSender.unregisterWithFlowScheduler();
    }

    private void getAndAssertAMQPFrames(int expectedFrameCount, String expectedDescriptor, int waitTime)
    {
        int flowFrameCount = 0;
        while (true)
        {
            try
            {
                ChannelBuffer channelBuffer = framesQueue.poll(waitTime, TimeUnit.MILLISECONDS);
                if (channelBuffer == null)
                {
                    break;
                }

                CAMQPSyncDecoder inputPipe = CAMQPSyncDecoder.createCAMQPSyncDecoder();
                inputPipe.take(channelBuffer);

                String controlName = inputPipe.readSymbol();
                assertTrue(controlName.equalsIgnoreCase(expectedDescriptor));
                flowFrameCount++;
                if (flowFrameCount == expectedFrameCount)
                {
                    break;
                }
            }
            catch (InterruptedException e)
            {
                assertFalse(true);
            }
        }
        assertTrue(flowFrameCount == expectedFrameCount);
    }
}
