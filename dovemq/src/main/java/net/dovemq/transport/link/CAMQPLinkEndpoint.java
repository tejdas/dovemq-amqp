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

import java.math.BigInteger;
import java.util.UUID;

import net.dovemq.transport.endpoint.CAMQPEndpointManager;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.CAMQPMessageDeliveryPolicy;
import net.dovemq.transport.endpoint.CAMQPEndpointPolicy.EndpointType;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.protocol.data.CAMQPConstants;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.protocol.data.CAMQPDefinitionSource;
import net.dovemq.transport.protocol.data.CAMQPDefinitionTarget;
import net.dovemq.transport.session.CAMQPSessionInterface;
import net.jcip.annotations.GuardedBy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class is extended by Link Sender and Link Receiver
 * implementations, to share the common logic around link
 * establishment, teardown and some common flow-control
 * attributes.
 *
 * @author tejdas
 */
public abstract class CAMQPLinkEndpoint implements CAMQPLinkMessageHandler, CAMQPLinkInterface {
    private static final Logger log = Logger.getLogger(CAMQPLinkEndpoint.class);

    private final String roleAsString;

    private String linkName;

    public String getLinkName() {
        return linkName;
    }

    protected final CAMQPLinkStateActor linkStateActor;

    protected long linkHandle;

    private long remoteLinkHandle = -1;

    private String sourceAddress;

    private String targetAddress;

    private CAMQPLinkKey linkKey;

    /*
     * Flow-control attributes
     */
    protected long deliveryCount = 0;

    protected long linkCredit = 0;

    protected long available = 0;

    protected final CAMQPSessionInterface session;

    protected volatile CAMQPDefinitionError linkClosedError = null;

    protected CAMQPEndpointPolicy endpointPolicy = CAMQPEndpointManager.getDefaultEndpointPolicy();

    public CAMQPEndpointPolicy getEndpointPolicy() {
        return endpointPolicy;
    }

    public CAMQPLinkEndpoint(CAMQPSessionInterface session) {
        super();
        this.session = session;
        this.roleAsString = (getRole() == LinkRole.LinkSender) ? "LinkSender" : "LinkReceiver";
        linkStateActor = new CAMQPLinkStateActor(this);
    }

    @Override
    public long getHandle() {
        return linkHandle;
    }

    @Override
    public CAMQPSessionInterface getSession() {
        return session;
    }

    /**
     * Establishes an AMQP link to a remote AMQP end-point.
     *
     * @param sourceName
     * @param targetName
     */
    void createLink(String sourceName, String targetName, CAMQPEndpointPolicy endpointPolicy) {
        this.endpointPolicy = endpointPolicy;
        sourceAddress = sourceName;
        targetAddress = targetName;

        linkHandle = CAMQPLinkManager.getNextLinkHandle();
        linkName = UUID.randomUUID().toString();
        CAMQPLinkManager.getLinkHandshakeTracker()
                .registerOutstandingLink(linkName, this);

        CAMQPControlAttach data = new CAMQPControlAttach();
        data.setHandle(linkHandle);
        data.setName(linkName);
        data.setRole(getRole() == LinkRole.LinkReceiver);

        CAMQPDefinitionSource source = new CAMQPDefinitionSource();
        source.setAddress(sourceAddress);
        if (endpointPolicy.getEndpointType() == EndpointType.TOPIC) {
            source.setDistributionMode(CAMQPConstants.STD_DIST_MODE_COPY);
        }
        data.setSource(source);

        CAMQPDefinitionTarget target = new CAMQPDefinitionTarget();
        target.setAddress(targetAddress);
        data.setTarget(target);

        source.setDynamic(false);
        if (getRole() == LinkRole.LinkSender) {
            data.setInitialDeliveryCount(deliveryCount);
        }

        /*
         * Populate the end-point policy info to the ATTACH frame.
         */
        data.setMaxMessageSize(BigInteger.valueOf(endpointPolicy.getMaxMessageSize()));
        data.setSndSettleMode(endpointPolicy.getSenderSettleMode());
        data.setRcvSettleMode(endpointPolicy.getReceiverSettleMode());

        /*
         * Populate custom Endpoint properties
         */
        if (!endpointPolicy.getCustomProperties().isEmpty()) {
            data.getProperties().putAll(endpointPolicy.getCustomProperties());
            data.setRequiredProperties(true);
        }

        linkStateActor.sendAttach(data);
        linkStateActor.waitForAttached(targetAddress);
    }

    /**
     * Closes an AMQP link
     */
    void destroyLink() {
        CAMQPControlDetach data = new CAMQPControlDetach();
        data.setClosed(true);
        data.setHandle(linkHandle);
        linkStateActor.sendDetach(data);
        linkStateActor.waitForDetached(targetAddress);
    }

    /**
     * Closes an AMQP link, specifying the reason for closure.
     *
     * @param error
     */
    public void destroyLink(CAMQPDefinitionError error, boolean waitForLinkClosure) {
        CAMQPControlDetach data = new CAMQPControlDetach();
        data.setError(error);
        data.setClosed(true);
        data.setHandle(linkHandle);
        linkStateActor.sendDetach(data);
        if (waitForLinkClosure) {
            linkStateActor.waitForDetached(targetAddress);
        }
    }

    /**
     * Dispatched by session layer when a Link attach frame is received from the
     * AMQP peer.
     */
    @Override
    public void attachReceived(CAMQPControlAttach data) {
        linkKey = CAMQPLinkKey.createLinkKey(data);
        linkStateActor.attachReceived(data);
    }

    /**
     * Dispatched by session layer when a Link detach frame is received from the
     * AMQP peer.
     */
    @Override
    public void detachReceived(CAMQPControlDetach data) {
        CAMQPDefinitionError error = data.getError();
        if (error != null) {
            linkClosedError = error;
            String errorDetails = errorToString(error);
            log.warn(errorDetails);
        }
        linkStateActor.detachReceived(data);
    }

    protected static String errorToString(CAMQPDefinitionError error) {
        StringBuilder errorDetails = new StringBuilder("Link detached by remote peer;");
        if (!StringUtils.isEmpty(error.getCondition())) {
            errorDetails.append(" Error condition:" + error.getCondition());
        }
        if (!StringUtils.isEmpty(error.getDescription())) {
            errorDetails.append(" Error description:" + error.getDescription());
        }
        return errorDetails.toString();
    }

    public void attached(boolean isInitiator) {
        if (linkKey != null) {
            CAMQPLinkManager.getLinkmanager()
                    .registerLinkEndpoint(linkName, linkKey, this);

            if (!isInitiator) {
                if (getRole() == LinkRole.LinkSender) {
                    CAMQPEndpointManager.sourceEndpointAttached(linkKey.getSource(), (CAMQPLinkSenderInterface) this, endpointPolicy);
                }
                else {
                    CAMQPEndpointManager.targetEndpointAttached(linkKey.getTarget(), (CAMQPLinkReceiverInterface) this, endpointPolicy);
                }
            }
        }
        String initiatedBy = isInitiator ? "self" : "peer";
        log.debug(roleAsString + " created between source: " + sourceAddress + " and target: " + targetAddress + " . Initiated by: " + initiatedBy);
    }

    public void detached(boolean isInitiator) {
        if (remoteLinkHandle != -1) {
            session.unregisterLinkReceiver(remoteLinkHandle);
        }
        if (linkKey != null) {
            CAMQPLinkManager.getLinkmanager()
                    .unregisterLinkEndpoint(linkName, linkKey);
            if (!isInitiator) {
                if (getRole() == LinkRole.LinkSender) {
                    CAMQPEndpointManager.sourceEndpointDetached(linkKey.getSource(), (CAMQPSourceInterface) getEndpoint(), endpointPolicy);
                }
                else {
                    CAMQPEndpointManager.targetEndpointDetached(linkKey.getTarget(), (CAMQPTargetInterface) getEndpoint(), endpointPolicy);
                }
            }
        }
        String initiatedBy = isInitiator ? "self" : "peer";
        log.debug(roleAsString + " destroyed between source: " + sourceAddress + " and target: " + targetAddress + " . Initiated by: " + initiatedBy);
    }

    /**
     * Process an incoming Link attach frame. For link establishment initiator,
     * nothing needs do be done. Otherwise, set the initial delivery count and
     * source or target address from the incoming attach frame. Send back an
     * attach frame,
     *
     * @param data
     * @param isInitiator
     */
    void processAttachReceived(CAMQPControlAttach data, boolean isInitiator) {
        remoteLinkHandle = data.getHandle();

        if (isInitiator)
            return;

        linkHandle = CAMQPLinkManager.getNextLinkHandle();

        linkName = data.getName();

        if (getRole() == LinkRole.LinkReceiver) {
            if (data.isSetInitialDeliveryCount())
                deliveryCount = data.getInitialDeliveryCount();
        }

        /*
         * Override endpointPolicy with that received in the ATTACH frame from
         * peer
         */
        long maxMessageSize = CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE;
        if (data.isSetMaxMessageSize()) {
            maxMessageSize = data.getMaxMessageSize().longValue();
        }

        int sndSettleMode = CAMQPConstants.SENDER_SETTLE_MODE_MIXED;
        if (data.isSetSndSettleMode()) {
            sndSettleMode = data.getSndSettleMode();
        }

        int rcvSettleMode = CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND;
        if (data.isSetRcvSettleMode()) {
            rcvSettleMode = data.getRcvSettleMode();
        }

        endpointPolicy = new CAMQPEndpointPolicy(maxMessageSize, sndSettleMode, rcvSettleMode, endpointPolicy);

        if (data.isSetProperties()) {
            endpointPolicy.getCustomProperties().putAll(data.getProperties());
        }

        CAMQPControlAttach responseData = new CAMQPControlAttach();
        responseData.setHandle(linkHandle);
        responseData.setName(linkName);
        responseData.setRole(getRole() == LinkRole.LinkReceiver);

        if (data.getSource() != null) {
            CAMQPDefinitionSource inSource = (CAMQPDefinitionSource) data.getSource();
            sourceAddress = (String) inSource.getAddress();

            if (inSource.isSetDistributionMode()) {
                String distMode = inSource.getDistributionMode();
                if (StringUtils.equalsIgnoreCase(distMode, CAMQPConstants.STD_DIST_MODE_COPY)) {
                    endpointPolicy.setEndpointType(EndpointType.TOPIC);
                    endpointPolicy.setLinkCreditPolicy(ReceiverLinkCreditPolicy.CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING);
                }
            }
        }

        if (data.getTarget() != null) {
            CAMQPDefinitionTarget inTarget = (CAMQPDefinitionTarget) data.getTarget();
            targetAddress = (String) inTarget.getAddress();
        }

        CAMQPDefinitionSource source = new CAMQPDefinitionSource();
        source.setAddress(sourceAddress);
        responseData.setSource(source);

        CAMQPDefinitionTarget target = new CAMQPDefinitionTarget();
        target.setAddress(targetAddress);
        responseData.setTarget(target);

        source.setDynamic(false);
        responseData.setInitialDeliveryCount(deliveryCount);
        responseData.setMaxMessageSize(BigInteger.valueOf(CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE));

        linkStateActor.sendAttach(responseData);
    }

    /**
     * Process an incoming Link detach frame. For link establishment initiator,
     * nothing needs do be done. Otherwise, send back an attach frame.
     *
     * @param data
     * @param isInitiator
     */
    void processDetachReceived(CAMQPControlDetach data, boolean isInitiator) {
        if (isInitiator)
            return;

        CAMQPControlDetach responseData = new CAMQPControlDetach();
        responseData.setClosed(true);
        responseData.setHandle(linkHandle);
        linkStateActor.sendDetach(responseData);
    }

    @GuardedBy("this")
    CAMQPControlFlow populateFlowFrame() {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
        flow.setAvailable(available);
        flow.setDeliveryCount(deliveryCount);
        flow.setLinkCredit(linkCredit);
        return flow;
    }

    @GuardedBy("this")
    CAMQPControlFlow populateFlowFrameAvailableUnknown() {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
        flow.setDeliveryCount(deliveryCount);
        flow.setLinkCredit(linkCredit);
        return flow;
    }

    /**
     * Sends disposition frame to the peer. If the current role is LinkReceiver,
     * it is sending the disposition for LinkSender, so set role to true If the
     * current role is LinkSender, it is sending the disposition for
     * LinkReceiver, so set role to false
     *
     * @param deliveryId
     * @param settleMode
     * @param newState
     */
    public void sendDisposition(long deliveryId, boolean settleMode, Object newState) {
        session.sendDisposition(deliveryId, settleMode, (getRole() == LinkRole.LinkReceiver), newState);
    }

    @Override
    public void sessionClosed() {
        linkStateActor.sessionClosed();
        detached(false); // TODO
    }

    abstract Object getEndpoint();

    /**
     * Send the message on the underlying AMQP session as a transfer frame.
     *
     * @param message
     * @param messageSource
     */
    void send(CAMQPMessage message, CAMQPSourceInterface messageSource) {
        /*
         * TODO: fragment the message into multiple transfer frames if the
         * message size is greater than the negotiated transfer frame size.
         */
        long deliveryId = session.getNextDeliveryId();
        CAMQPControlTransfer transferFrame = new CAMQPControlTransfer();
        transferFrame.setDeliveryId(deliveryId);
        transferFrame.setMore(false);
        transferFrame.setHandle(linkHandle);
        transferFrame.setDeliveryTag(message.getDeliveryTag().getBytes());

        populateTransferFrameWithDispositionPolicy(transferFrame, endpointPolicy.getDeliveryPolicy());

        /*
         * Notify the source end-point that the message is about to be sent.
         */
        if (messageSource != null) {
            messageSource.messageSent(deliveryId, message);
        }

        assert (this instanceof CAMQPLinkSenderInterface);
        session.sendTransfer(transferFrame, message.getPayload(), (CAMQPLinkSenderInterface) this);
    }

    private static void populateTransferFrameWithDispositionPolicy(CAMQPControlTransfer transferFrame, CAMQPMessageDeliveryPolicy deliveryPolicy) {
        boolean senderSettled = (deliveryPolicy == CAMQPMessageDeliveryPolicy.AtmostOnce);
        int receiverSettledMode = (deliveryPolicy == CAMQPMessageDeliveryPolicy.ExactlyOnce) ? CAMQPConstants.RECEIVER_SETTLE_MODE_SECOND : CAMQPConstants.RECEIVER_SETTLE_MODE_FIRST;
        transferFrame.setSettled(senderSettled);
        transferFrame.setRcvSettleMode(receiverSettledMode);
    }
}
