package net.dovemq.transport.endpoint;

import java.util.Random;

import net.dovemq.api.DoveMQMessage;

import org.apache.commons.lang.RandomStringUtils;

public class EndpointTestUtils
{
    public static DoveMQMessage createEncodedMessage(Random randomGenerator)
    {
        int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
        String str = RandomStringUtils.randomAlphanumeric(sectionSize);
        byte[] payload = str.getBytes();
        DoveMQMessageImpl message = new DoveMQMessageImpl();
        message.addPayload(payload);
        return message;
    }
}
