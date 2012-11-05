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

import java.util.Collection;

import net.dovemq.api.DoveMQEndpointPolicy.MessageAcknowledgementPolicy;

public class ConsumerAckExplicitTest
{
    private static class TestMessageReceiver implements DoveMQMessageReceiver
    {
        public TestMessageReceiver(Consumer consumer)
        {
            super();
            this.consumer = consumer;
        }
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            Collection<byte[]> body = message.getPayloads();
            for (byte[] b : body)
            {
                String bString = new String(b);
                System.out.println(bString);
            }
            consumer.acknowledge(message);
        }
        private final Consumer consumer;
    }
    public static void main(String[] args) throws InterruptedException
    {
        String brokerIP = args[0];
        String endpointName = args[1];
        String queueName = args[2];
        ConnectionFactory.initialize(endpointName);

        Session session = ConnectionFactory.createSession(brokerIP);

        Consumer consumer = session.createConsumer(queueName, new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS));
        consumer.registerMessageReceiver(new TestMessageReceiver(consumer));
        System.out.println("waiting for message");
        Thread.sleep(30000);

        //session.close();
        ConnectionFactory.shutdown();
    }
}
