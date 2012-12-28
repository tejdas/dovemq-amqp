package net.dovemq.transport.endpoint;

import java.util.Random;

import net.dovemq.api.DoveMQMessage;
import net.dovemq.transport.link.LinkTestMultipleSources;

import org.apache.commons.lang.RandomStringUtils;

public class EndpointTestUtils
{
    /**
      * Since the functional test {@link LinkTestMultipleSources}
      * uses NUM_THREADS=5 and
      * numMessagesToSend=50000, the test fails on Windows
      * because of repeated calls to
      * RandomStringUtils.randomAlphanumeric()
      *
      * The boolean generateRandomString determines whether to call
      * RandomStringUtils.randomAlphanumeric()
      *
      * @param randomGenerator
      * @param generateRandomString
      * @return
      */
    public static DoveMQMessage createEncodedMessage(Random randomGenerator, boolean generateRandomString)
    {
        int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
        byte[] payload;

        if (generateRandomString)
        {
            String str = RandomStringUtils.randomAlphanumeric(sectionSize);
            payload = str.getBytes();
        }
        else
        {
            payload = new byte[sectionSize];
        }

        DoveMQMessageImpl message = new DoveMQMessageImpl();
        message.addPayload(payload);
        return message;
    }

    public static DoveMQMessage createSmallEncodedMessage(Random randomGenerator, boolean generateRandomString)
    {
        int sectionSize = (randomGenerator.nextInt(10) + 1);
        byte[] payload;

        if (generateRandomString)
        {
            String str = RandomStringUtils.randomAlphanumeric(sectionSize);
            payload = str.getBytes();
        }
        else
        {
            payload = new byte[sectionSize];
        }

        DoveMQMessageImpl message = new DoveMQMessageImpl();
        message.addPayload(payload);
        return message;
    }
}
