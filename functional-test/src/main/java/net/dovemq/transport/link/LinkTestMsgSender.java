package net.dovemq.transport.link;

import java.util.concurrent.CountDownLatch;

import net.dovemq.transport.common.CAMQPTestTask;

public class LinkTestMsgSender extends CAMQPTestTask implements Runnable
{
    private volatile CAMQPLinkSender linkSender = null;
    CAMQPLinkSender getLinkSender()
    {
        return linkSender;
    }

    private final String linkSource;
    private final String linkTarget;
    private final String containerId;
    private final int numMessagesToSend;
    private final LinkCommandMBean mbeanProxy;
    public LinkTestMsgSender(CountDownLatch startSignal,
            CountDownLatch doneSignal, String src, String target, String containerId, int numMessagesToSend, LinkCommandMBean mbeanProxy)
    {
        super(startSignal, doneSignal);
        this.linkSource = src;
        this.linkTarget = target;
        this.containerId = containerId;
        this.numMessagesToSend = numMessagesToSend;
        this.mbeanProxy = mbeanProxy;
    }

    @Override
    public void run()
    {
        CAMQPLinkSender sender = CAMQPLinkFactory.createLinkSender(containerId, linkSource, linkTarget);
        linkSender = sender;
        System.out.println("Sender Link created between : " + linkSource + "  and: " + linkTarget);
        
        String linkName = linkSender.getLinkName();
        
        mbeanProxy.registerTarget(linkSource, linkTarget);
        mbeanProxy.issueLinkCredit(linkName, 10);
        
        LinkTestUtils.sendMessagesOnLink(linkSender, numMessagesToSend);
        waitForReady();
        linkSender.destroyLink();
        done();
    }
}
