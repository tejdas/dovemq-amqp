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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.connection.CAMQPConnectionProperties;
import net.dovemq.transport.connection.CAMQPListener;
import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.dovemq.transport.session.CAMQPSessionManager;

import org.apache.log4j.Logger;

/**
 * This class is used as a manager for AMQP links.
 * @author tejdas
 *
 */
public final class CAMQPLinkManager implements CAMQPLinkMessageHandlerFactory {
    static final class LinkHandshakeTracker {
        private final Map<String, CAMQPLinkMessageHandler> outstandingLinks = new ConcurrentHashMap<>();

        /**
         * If we are the initiators of the Link establishment handshake, we
         * first register the link end-point here. It will be removed in
         * linkAccepted, upon completion of the link establishment.
         *
         * @param linkName
         * @param linkEndpoint
         */
        void registerOutstandingLink(String linkName, CAMQPLinkMessageHandler linkEndpoint) {
            outstandingLinks.put(linkName, linkEndpoint);
        }

        CAMQPLinkMessageHandler unregisterOutstandingLink(String linkName) {
            return outstandingLinks.remove(linkName);
        }
    }

    public static enum LinkSenderType {
        PUSH, PULL
    }

    private static final Logger log = Logger.getLogger(CAMQPLinkManager.class);

    private static final AtomicLong nextLinkHandle = new AtomicLong(0);

    private static final CAMQPLinkManager linkManager = new CAMQPLinkManager();

    private static volatile CAMQPListener listener = null;

    public static CAMQPLinkManager getLinkmanager() {
        return linkManager;
    }

    private final CAMQPLinkSendFlowScheduler flowScheduler = new CAMQPLinkSendFlowScheduler();

    CAMQPLinkSendFlowScheduler getFlowScheduler() {
        return flowScheduler;
    }

    private static LinkSenderType linkSenderType = LinkSenderType.PUSH;

    public static void setLinkSenderType(LinkSenderType linkSenderType) {
        CAMQPLinkManager.linkSenderType = linkSenderType;
    }

    private final LinkHandshakeTracker linkHandshakeTracker = new LinkHandshakeTracker();

    private final ConcurrentMap<String, CAMQPLinkEndpoint> openLinks = new ConcurrentHashMap<>();

    private final ConcurrentMap<CAMQPLinkKey, Set<String>> keyToLinkSets = new ConcurrentHashMap<>();

    static LinkHandshakeTracker getLinkHandshakeTracker() {
        return linkManager.linkHandshakeTracker;
    }

    static long getNextLinkHandle() {
        return nextLinkHandle.getAndIncrement();
    }

    public static void initialize(boolean isBroker, String containerId) {
        CAMQPConnectionManager.initialize(containerId);
        CAMQPConnectionManager.registerConnectionObserver(new CAMQPConnectionReaper());
        log.info("container ID: " + CAMQPConnectionManager.getContainerId());

        if (isBroker) {
            CAMQPConnectionProperties defaultConnectionProps = CAMQPConnectionProperties.createConnectionProperties();
            listener = CAMQPListener.createCAMQPListener(defaultConnectionProps);
        }
        CAMQPSessionManager.initialize();
        CAMQPSessionManager.registerLinkReceiverFactory(linkManager);
        linkManager.flowScheduler.start();

        if (isBroker) {
            listener.start();
        }
    }

    public static void shutdown() {
        linkManager.flowScheduler.stop();
        linkManager.shutdownLinks();
        CAMQPSessionManager.shutdown();
        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
        if (listener != null) {
            listener.shutdown();
        }
    }

    /*
     * Used for API functional tests
     */
    public CAMQPTargetInterface attachLinkTargetEndpoint(String linkSource, String linkTarget) {
        CAMQPLinkEndpoint linkEndpoint = CAMQPLinkManager.getLinkmanager()
                .getLinkEndpoint(linkSource, linkTarget);
        if (linkEndpoint == null) {
            log.warn("could not find link endpoint for: " + linkSource + "." + linkTarget);
            return null;
        }
        if (linkEndpoint.getRole() == LinkRole.LinkReceiver) {
            return CAMQPEndpointManager.targetEndpointAttached(linkTarget, (CAMQPLinkReceiverInterface) linkEndpoint, linkEndpoint.getEndpointPolicy());
        }
        else {
            log.warn("LinkEndpoint is not a LinkReceiver");
        }
        return null;
    }

    /*
     * Used for API functional tests
     */
     CAMQPLinkEndpoint getLinkEndpoint(String source, String target) {
        CAMQPLinkKey linkKey = new CAMQPLinkKey(source, target);

        String linkName = null;
        synchronized (this) {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if ((linkSetByKey != null) && (!linkSetByKey.isEmpty())) {
                Iterator<String> iter = linkSetByKey.iterator();
                if (iter.hasNext()) {
                    linkName = iter.next();
                }
            }
        }

        if (linkName != null) {
            return openLinks.get(linkName);
        }
        return null;
    }

    /*
     * Used for API functional tests
     */
    public CAMQPLinkEndpoint getLinkEndpoint(String linkName) {
        return openLinks.get(linkName);
    }

    /**
     * Called by Session layer upon the receipt of a Link attach frame.
     */
    @Override
    public CAMQPLinkMessageHandler linkAccepted(CAMQPSessionInterface session, CAMQPControlAttach attach) {
        /*
         * role denotes the role of the Peer that has sent the ATTACH frame
         */
        Boolean role = attach.getRole();
        String linkName = attach.getName();
        CAMQPLinkMessageHandler linkEndpoint = linkHandshakeTracker.unregisterOutstandingLink(linkName);
        if (linkEndpoint != null) {
            /*
             * We initiated the Link establishment handshake (via
             * CAMQPLinkEndpoint.createLink()) The Link establishment is
             * complete. Remove and return the previously registered link
             * endpoint.
             */
            return linkEndpoint;
        }

        /*
         * We are the link receptors. Create an appropriate link end-point,
         * based on the role of the peer.
         */
        if (role == CAMQPLinkConstants.ROLE_RECEIVER) {
            /*
             * Peer is a Link receiver.
             */
            if (linkSenderType == LinkSenderType.PUSH) {
                return new CAMQPLinkSender(session);
            }
            else {
                return new CAMQPLinkAsyncSender(session);
            }
        }
        else {
            /*
             * Peer is a Link sender.
             */
            return new CAMQPLinkReceiver(session);
        }
    }

    void registerLinkEndpoint(String linkName, CAMQPLinkKey linkKey, CAMQPLinkEndpoint linkEndpoint) {
        log.debug("registerLinkEndpoint: linkName: " + linkName + "  LinkKey: " + linkKey.toString());
        openLinks.put(linkName, linkEndpoint);

        synchronized (this) {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if (linkSetByKey == null) {
                linkSetByKey = new LinkedHashSet<>();
                keyToLinkSets.put(linkKey, linkSetByKey);
            }
            linkSetByKey.add(linkName);
        }
    }

    void unregisterLinkEndpoint(String linkName, CAMQPLinkKey linkKey) {
        log.debug("unregisterLinkEndpoint: linkName: " + linkName + "  LinkKey: " + linkKey.toString());
        synchronized (this) {
            Set<String> linkSetByKey = keyToLinkSets.get(linkKey);
            if (linkSetByKey != null) {
                linkSetByKey.remove(linkName);
                if (linkSetByKey.isEmpty()) {
                    keyToLinkSets.remove(linkKey);
                }
            }
        }
        openLinks.remove(linkName);
    }

    private void shutdownLinks() {
        Set<String> linksByName = openLinks.keySet();
        for (String linkName : linksByName) {
            CAMQPLinkEndpoint linkEndpoint = openLinks.remove(linkName);
            if (linkEndpoint != null) {
                linkEndpoint.destroyLink();
            }
        }
    }
}
