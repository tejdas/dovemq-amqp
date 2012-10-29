package net.dovemq.transport.endpoint;

import java.util.Random;

import net.dovemq.api.DoveMQMessage;

public class EndpointTestUtils
{
    public static DoveMQMessage createEncodedMessage(Random randomGenerator)
    {
        int sectionSize = 256 * (randomGenerator.nextInt(10) + 1);
        //String str = RandomStringUtils.randomAlphanumeric(sectionSize);
        String str = "Tejeswar Das. 21230 Homestead Road. Apt-53. Cupertino, CA - 95014";
        byte[] payload = str.getBytes();
        DoveMQMessageImpl message = new DoveMQMessageImpl();
        message.addPayload(payload);
        return message;
    }
}
