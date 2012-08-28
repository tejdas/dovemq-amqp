package net.dovemq.transport.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.link.CAMQPLinkMessageHandlerFactory;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;

public class SysTestCommandReceiverFactory implements CAMQPLinkMessageHandlerFactory
{
    private final Set<SysBaseLinkReceiver> linkReceivers = Collections.synchronizedSet(new HashSet<SysBaseLinkReceiver>());
    
    private void add(SysBaseLinkReceiver linkReceiver)
    {
        linkReceivers.add(linkReceiver);
    }
    
    void remove(SysBaseLinkReceiver linkReceiver)
    {
        linkReceivers.remove(linkReceiver);
    }
    
    public boolean isDone()
    {
        synchronized (linkReceivers)
        {
            for (SysBaseLinkReceiver linkReceiver : linkReceivers)
            {
                if (!linkReceiver.isDone())
                    return false;
            }
        }
        return true;
    }
    
    protected static Collection<String> getAvailableFactoryList()
    {
        return Arrays.asList
        (
            "SysTestCommandReceiver",
            "SysTestDelayedAckLinkReceiver"
        );
    }
    private String commandReceiverClassName = null;
    protected String getCommandReceiverClassName()
    {
        return commandReceiverClassName;
    }
    protected SysTestCommandReceiverFactory(String className)
    {
        commandReceiverClassName = className;
    }
    CAMQPLinkMessageHandler currentCommandReceiver = null;
    @Override
    public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach)
    {
        SysBaseLinkReceiver linkReceiver = null;
        if (commandReceiverClassName.equalsIgnoreCase("SysTestCommandReceiver"))
        {
            linkReceiver = new SysTestCommandReceiver(session);
        }
        else if (commandReceiverClassName.equalsIgnoreCase("SysTestDelayedAckLinkReceiver"))
        {
            linkReceiver = new SysDelayedAckLinkReceiver(session);
            new Thread((SysDelayedAckLinkReceiver) linkReceiver).start();
        }
        else
        {
            System.out.println("Unknown CAMQPCommandReceiver");
            return null;
        }
        add(linkReceiver);
        linkReceiver.registerFactory(this);
        
        return linkReceiver;
    }   
}
