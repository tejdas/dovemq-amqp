package net.dovemq.transport.link;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.RandomStringUtils;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.frame.CAMQPMessagePayload;

public class LinkTestMessageSender extends CAMQPTestTask implements Runnable
{
    private final CAMQPLinkSender linkSender;
    private final int numMessagesToSend;
    public LinkTestMessageSender(CountDownLatch startSignal,
            CountDownLatch doneSignal, CAMQPLinkSender linkSender, int numMessagesToSend)
    {
        super(startSignal, doneSignal);
        this.linkSender = linkSender;
        this.numMessagesToSend = numMessagesToSend;
    }

    @Override
    public void run()
    {
        waitForReady();
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
        done();
    }
}
