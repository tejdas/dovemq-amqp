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

package net.dovemq.api;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PublisherTest
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        String brokerIP = args[0];
        String endpointName = args[1];
        String topicName = args[2];
        String fileName = args[3];
        int numIterations = Integer.parseInt(args[4]);

        ConnectionFactory.initialize(endpointName);

        Session session = ConnectionFactory.createSession(brokerIP);
        System.out.println("created session");

        Publisher publisher = session.createPublisher(topicName);

        final AtomicInteger messageAckCount = new AtomicInteger(0);
        publisher.registerMessageAckReceiver(new DoveMQMessageAckReceiver() {

            @Override
            public void messageAcknowledged(DoveMQMessage message)
            {
                messageAckCount.incrementAndGet();
            }
        });

        System.out.println("created publisher");

        Thread.sleep(10000);

        String sourceName = System.getenv("DOVEMQ_TEST_DIR") + "/" + fileName;
        int messagesSent = 0;
        for (int i = 0; i < numIterations; i++)
        {
            messagesSent += sendFileContents(sourceName, publisher);
        }

        /*
         * Send final message
         */
        publisher.publishMessage("TOPIC_TEST_DONE".getBytes());
        messagesSent++;

        while (messageAckCount.get() < messagesSent)
        {
            try
            {
                Thread.sleep(5000);
                System.out.println("publisher waiting: " + messagesSent + " " + messageAckCount.get());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        ConnectionFactory.shutdown();
    }

    private static int sendFileContents(String fileName, Publisher publisher) throws IOException
    {
        int messageCount = 0;
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            publisher.publishMessage(message);
            messageCount++;
        }
        freader.close();
        return messageCount;
    }
}
