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

import java.util.Random;
import java.util.UUID;

import net.dovemq.transport.frame.CAMQPMessagePayload;

import org.apache.commons.lang.RandomStringUtils;

public class LinkTestUtils
{
    public static void sendMessagesOnLink(CAMQPLinkSender linkSender, int numMessagesToSend)
    {
        Random randomGenerator = new Random();
        for (int i = 0; i < numMessagesToSend; i++)
        {
            int randomInt = randomGenerator.nextInt(5);
            CAMQPMessage message = createMessage(randomGenerator);
            linkSender.sendMessage(message.getDeliveryTag(), message.getPayload());
            try
            {
                Thread.sleep(randomInt);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static CAMQPMessage createMessage(Random randomGenerator)
    {
        String deliveryTag = UUID.randomUUID().toString();
        int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
        String str = RandomStringUtils.randomAlphanumeric(sectionSize);
        CAMQPMessagePayload payload = new CAMQPMessagePayload(str.getBytes());
        return new CAMQPMessage(deliveryTag, payload);
    }

    public static CAMQPMessagePayload createMessagePayload(Random randomGenerator)
    {
        int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
        String str = RandomStringUtils.randomAlphanumeric(sectionSize);
        return new CAMQPMessagePayload(str.getBytes());
    }
}
