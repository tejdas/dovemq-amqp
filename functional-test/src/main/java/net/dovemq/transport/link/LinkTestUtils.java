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
            int randomInt = randomGenerator.nextInt(20);
            String deliveryTag = UUID.randomUUID().toString();
            int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
            String str = RandomStringUtils.randomAlphanumeric(sectionSize);
            CAMQPMessagePayload payload = new CAMQPMessagePayload(str.getBytes());            
            linkSender.sendMessage(deliveryTag, payload);
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
}
