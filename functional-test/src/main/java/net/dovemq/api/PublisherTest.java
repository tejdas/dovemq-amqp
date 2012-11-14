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

public class PublisherTest
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        String brokerIP = args[0];
        String endpointName = args[1];
        String topicName = args[2];
        String fileName = args[3];
        ConnectionFactory.initialize(endpointName);

        Session session = ConnectionFactory.createSession(brokerIP);
        System.out.println("created session");

        Publisher publisher = session.createPublisher(topicName);
        System.out.println("created publisher");

        Thread.sleep(10000);

        String sourceName = System.getenv("DOVEMQ_TEST_DIR") + "/" + fileName;
        sendFileContents(sourceName, publisher);

        System.out.println("publisher sleeping for 30 secs");
        Thread.sleep(30000);

        //session.close();
        ConnectionFactory.shutdown();
    }

    private static void sendFileContents(String fileName, Publisher publisher) throws IOException
    {
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            publisher.sendMessage(message);
        }
        freader.close();
    }
}