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

public interface LinkCommandMBean {
    public void registerFactory(String factoryName);

    public void registerSource(String linkSource,
            String linkTarget,
            long initialMessageCount);

    public void registerTarget(String linkSource, String linkTarget);

    public void registerDelayedTarget(String linkSource,
            String linkTarget,
            int averageMsgProcessingTime);

    public Collection<String> getLinks();

    public void createSenderLink(String source,
            String target,
            String remoteContainerId);

    public void setLinkCreditSteadyState(String linkName,
            long minLinkCreditThreshold,
            long linkCreditBoost,
            ReceiverLinkCreditPolicy policy);

    public void issueLinkCredit(String linkName, long linkCreditBoost);

    public long getNumMessagesReceived();

    public long getNumMessagesProcessedByDelayedEndpoint();

    public boolean processedAllMessages();

    public void reset();

    public void attachTarget(String linkSource, String linkTarget);

    public void attachSharedTarget(String linkSource, String linkTarget);

    public long getNumMessagesReceivedAtTargetReceiver();

    public void createSource(String source,
            String target,
            String remoteContainerId);
}
