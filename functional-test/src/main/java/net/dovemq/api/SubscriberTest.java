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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

public class SubscriberTest
{
    private static volatile PrintWriter fw = null;
    private static class TestMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            Collection<byte[]> body = message.getPayloads();
            for (byte[] b : body)
            {
                String bString = new String(b);
                fw.println(bString);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException
    {
        String brokerIP = args[0];
        String endpointName = args[1];
        String topicName = args[2];

        fw = new PrintWriter(endpointName + ".txt");

        ConnectionFactory.initialize(endpointName);

        Session session = ConnectionFactory.createSession(brokerIP);

        Subscriber subscriber = session.createSubscriber(topicName);

        subscriber.registerMessageReceiver(new TestMessageReceiver());
        System.out.println("waiting for message");
        Thread.sleep(30000);

        fw.flush();
        fw.close();

        //session.close();
        ConnectionFactory.shutdown();
    }
}
