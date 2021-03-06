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

package net.dovemq.transport.endpoint;

import net.dovemq.broker.endpoint.DoveMQEndpointManager;
import net.dovemq.transport.link.CAMQPLinkFactory;
import net.dovemq.transport.link.CAMQPLinkReceiverInterface;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.session.CAMQPSessionInterface;

public final class CAMQPEndpointManager {
    private static volatile CAMQPEndpointPolicy defaultEndpointPolicy = new CAMQPEndpointPolicy();

    private static DoveMQEndpointManager doveMQEndpointManager = null;

    public static void registerDoveMQEndpointManager(DoveMQEndpointManager doveMQEndpointManager) {
        CAMQPEndpointManager.doveMQEndpointManager = doveMQEndpointManager;
    }

    public static CAMQPEndpointPolicy getDefaultEndpointPolicy() {
        return defaultEndpointPolicy;
    }

    public static void setDefaultEndpointPolicy(CAMQPEndpointPolicy defaultEndpointPolicy) {
        CAMQPEndpointManager.defaultEndpointPolicy = defaultEndpointPolicy;
    }

    /*
     * Used for API functional tests
     */
    public static CAMQPSourceInterface createSource(String containerId, String source, String target, CAMQPEndpointPolicy endpointPolicy) {
        CAMQPLinkSenderInterface linkSender = CAMQPLinkFactory.createLinkSender(containerId, source, target, endpointPolicy);
        CAMQPSource dovemqSource = new CAMQPSource(linkSender, endpointPolicy);
        linkSender.registerSource(dovemqSource);
        return dovemqSource;
    }

    public static CAMQPSourceInterface createSource(CAMQPSessionInterface session, String source, String target, CAMQPEndpointPolicy endpointPolicy) {
        CAMQPLinkSenderInterface linkSender = CAMQPLinkFactory.createLinkSender(session, source, target, endpointPolicy);
        CAMQPSource dovemqSource = new CAMQPSource(linkSender, endpointPolicy);
        linkSender.registerSource(dovemqSource);
        return dovemqSource;
    }

    public static CAMQPTargetInterface createTarget(CAMQPSessionInterface session, String source, String target, CAMQPEndpointPolicy endpointPolicy) {
        CAMQPLinkReceiverInterface linkReceiver = CAMQPLinkFactory.createLinkReceiver(session, source, target, endpointPolicy);
        CAMQPTarget dovemqTarget = new CAMQPTarget(linkReceiver, endpointPolicy);
        linkReceiver.registerTarget(dovemqTarget);
        linkReceiver.provideLinkCredit();
        return dovemqTarget;
    }

    public static CAMQPSourceInterface sourceEndpointAttached(String source, CAMQPLinkSenderInterface linkSender, CAMQPEndpointPolicy endpointPolicy) {
        CAMQPSource dovemqSource = new CAMQPSource(linkSender, endpointPolicy);
        linkSender.registerSource(dovemqSource);
        if (doveMQEndpointManager != null) {
            doveMQEndpointManager.sourceEndpointAttached(source, dovemqSource, endpointPolicy);
        }
        return dovemqSource;
    }

    public static CAMQPTargetInterface targetEndpointAttached(String target, CAMQPLinkReceiverInterface linkReceiver, CAMQPEndpointPolicy endpointPolicy) {
        CAMQPTarget dovemqTarget = new CAMQPTarget(linkReceiver, endpointPolicy);
        linkReceiver.registerTarget(dovemqTarget);
        linkReceiver.provideLinkCredit();
        if (doveMQEndpointManager != null) {
            doveMQEndpointManager.targetEndpointAttached(target, dovemqTarget, endpointPolicy);
        }
        return dovemqTarget;
    }

    public static void sourceEndpointDetached(String sourceName, CAMQPSourceInterface source, CAMQPEndpointPolicy endpointPolicy) {
        if (doveMQEndpointManager != null) {
            doveMQEndpointManager.sourceEndpointDetached(sourceName, source, endpointPolicy);
        }
    }

    public static void targetEndpointDetached(String targetName, CAMQPTargetInterface target, CAMQPEndpointPolicy endpointPolicy) {
        if (doveMQEndpointManager != null) {
            doveMQEndpointManager.targetEndpointDetached(targetName, target, endpointPolicy);
        }
    }
}
