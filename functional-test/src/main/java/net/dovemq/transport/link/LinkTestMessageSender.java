package net.dovemq.transport.link;

import java.util.concurrent.CountDownLatch;

import net.dovemq.transport.common.CAMQPTestTask;

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
        LinkTestUtils.sendMessagesOnLink(linkSender, numMessagesToSend);
        done();
    }
}
