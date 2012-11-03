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

public class ProducerTest
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        ConnectionFactory.initialize("producer");
        String brokerIP = args[0];
        String queueName = args[1];

        Session session = ConnectionFactory.createSession(brokerIP);
        System.out.println("created session");

        Producer producer = session.createProducer(queueName);
        System.out.println("created producer");

        DoveMQMessage message = MessageFactory.createMessage();
        String payload = "Hello World";
        message.addPayload(payload.getBytes());
        producer.sendMessage(message);
        System.out.println("sent message");

        String sourceName = System.getenv("DOVEMQ_TEST_DIR") + "/build.xml";
        sendFileContents(sourceName, producer);

        Thread.sleep(20000);

        //session.close();
        ConnectionFactory.shutdown();
    }

    private static void sendFileContents(String fileName, Producer producer) throws IOException
    {
        BufferedReader freader = new BufferedReader(new FileReader(fileName));
        String sLine = null;
        while ((sLine = freader.readLine()) != null)
        {
            DoveMQMessage message = MessageFactory.createMessage();
            message.addPayload(sLine.getBytes());
            producer.sendMessage(message);
        }
        freader.close();
    }
}
