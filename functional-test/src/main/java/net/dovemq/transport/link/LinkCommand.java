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

import java.util.Collection;

import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.ReceiverLinkCreditPolicy;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

public class LinkCommand implements LinkCommandMBean
{
    private volatile LinkTestTarget linkTargetEndpoint = new LinkTestTarget();

    private volatile LinkTestDelayedTarget linkDelayedTargetEndpoint = null;
    private volatile LinkTestTargetReceiver linkTargetReceiver = null;
    private volatile CAMQPSourceInterface linkSource = null;
    private final LinkTestTargetReceiver linkTargetSharedReceiver = new LinkTestTargetReceiver();

    @Override
    public void registerFactory(String factoryName)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void registerSource(String linkSource, String linkTarget, long initialMessageCount)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkSender)
        {
            CAMQPLinkAsyncSender linkSender = (CAMQPLinkAsyncSender) linkEndpoint;
            linkSender.setSource(new LinkTestSource(initialMessageCount));
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkSender");
        }
    }

    @Override
    public void registerTarget(String linkSource, String linkTarget)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiver linkReceiver = (CAMQPLinkReceiver) linkEndpoint;

            linkReceiver.setTarget(linkTargetEndpoint);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");
        }
    }

    @Override
    public Collection<String> getLinks()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSenderLink(String source,
            String target,
            String remoteContainerId)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLinkCreditSteadyState(String linkName, long minLinkCreditThreshold, long linkCreditBoost, ReceiverLinkCreditPolicy policy)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkName);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiverInterface linkReceiver = (CAMQPLinkReceiverInterface) linkEndpoint;
            if (policy == ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE)
                linkReceiver.configureSteadyStatePacedByMessageReceipt(minLinkCreditThreshold, linkCreditBoost);
            else
                linkReceiver.configureSteadyStatePacedByMessageProcessing(minLinkCreditThreshold, linkCreditBoost);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");
        }
    }

    @Override
    public void issueLinkCredit(String linkName, long linkCreditBoost)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkName);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiverInterface linkReceiver = (CAMQPLinkReceiverInterface) linkEndpoint;
            linkReceiver.issueLinkCredit(linkCreditBoost);
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");
        }
    }

    @Override
    public long getNumMessagesReceived()
    {
        return linkTargetEndpoint.getNumberOfMessagesReceived();
    }

    @Override
    public void reset()
    {
        linkTargetEndpoint.resetNumberOfMessagesReceived();
        if (linkTargetReceiver != null)
            linkTargetReceiver.stop();
        linkSource = null;
        linkTargetReceiver = null;
        linkTargetSharedReceiver.stop();
        if (linkDelayedTargetEndpoint != null)
        {
            linkDelayedTargetEndpoint.resetNumberOfMessagesReceived();
            linkDelayedTargetEndpoint.stopProcessing();
            linkDelayedTargetEndpoint = null;
        }
    }

    @Override
    public void attachTarget(String source, String target)
    {
        linkTargetReceiver = new LinkTestTargetReceiver();
        CAMQPTargetInterface linkTarget = CAMQPEndpointManager.attachTarget(source, target);
        linkTarget.registerTargetReceiver(linkTargetReceiver);
    }

    @Override
    public long getNumMessagesReceivedAtTargetReceiver()
    {
        if (linkTargetReceiver != null)
            return linkTargetReceiver.getNumberOfMessagesReceived();
        else
            return linkTargetSharedReceiver.getNumberOfMessagesReceived();
    }

    @Override
    public void createSource(String source,
            String target,
            String remoteContainerId)
    {
        linkSource = CAMQPEndpointManager.createSource(remoteContainerId, source, target, new CAMQPEndpointPolicy());
        if (linkTargetReceiver != null)
            linkTargetReceiver.setSource(linkSource);
    }

    @Override
    public void attachSharedTarget(String source, String target)
    {
        CAMQPTargetInterface linkTarget = CAMQPEndpointManager.attachTarget(source, target);
        linkTarget.registerTargetReceiver(linkTargetSharedReceiver);
    }

    @Override
    public void registerDelayedTarget(String linkSource, String linkTarget, int averageMsgProcessingTime)
    {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager().getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null)
        {
            System.out.println("could not find linkEndpoint");
            return;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver)
        {
            CAMQPLinkReceiver linkReceiver = (CAMQPLinkReceiver) linkEndpoint;
            linkDelayedTargetEndpoint = new LinkTestDelayedTarget(linkReceiver, averageMsgProcessingTime);
            linkReceiver.setTarget(linkDelayedTargetEndpoint);
            linkDelayedTargetEndpoint.startProcessing();
        }
        else
        {
            System.out.println("LinkEndpoint is not a LinkReceiver");
        }
    }

    @Override
    public long getNumMessagesProcessedByDelayedEndpoint()
    {
        if (linkDelayedTargetEndpoint != null)
            return linkDelayedTargetEndpoint.getNumberOfMessagesReceived();
        return 0;
    }

    @Override
    public boolean processedAllMessages()
    {
        if (linkDelayedTargetEndpoint != null)
            return linkDelayedTargetEndpoint.isDone();
        return false;
    }
}
