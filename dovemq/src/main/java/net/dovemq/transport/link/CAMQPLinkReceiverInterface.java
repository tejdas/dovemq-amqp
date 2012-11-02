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

import net.dovemq.transport.endpoint.CAMQPEndpointPolicy;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;

/**
 * Link layer interface that is used by target end-point
 * to configure various flow-control behaviors and to
 * start/stop incoming message flow on the link.
 *
 * @author tejdas
 *
 */
public interface CAMQPLinkReceiverInterface
{
    /**
     * Called by end-point layer to register a {@link CAMQPTargetInterface}.
     *
     * @param target: to be used for dispatching up the stack to the end-point
     * layer.
     */
    public void registerTarget(CAMQPTargetInterface target);

    /**
     * Called by target end-point to issue a one-time link credit.
     *
     * @param linkCreditBoost
     */
    public void issueLinkCredit(long linkCreditBoost);

    /**
     * Called by target end-point to issue a one-time link credit,
     * and to drain if not enough messages are available at peer.
     *
     * @param messageCount
     */
    public void getMessages(int messageCount);

    /**
     * Called by target end-point to configure the link credit policy
     * to {@link CAMQPEndpointPolicy.ReceiverLinkCreditPolicy#CREDIT_STEADY_STATE}
     * for a steady-state flow of incoming messages.
     *
     * It also immediately boosts the link credit if necessary.
     * Subsequently, whenever the link sender's link credit drops below
     * the configurable threshold, the link credit is boosted, and a flow
     * frame generated.
     *
     * @param minLinkCreditThreshold: the link credit at which the credit is boosted.
     * @param linkCreditBoost: amount of link credit to boost.
     */
    public void configureSteadyStatePacedByMessageReceipt(long minLinkCreditThreshold, long linkCreditBoost);

    /**
     * Called by target end-point to configure the link credit policy
     * to {@link CAMQPEndpointPolicy.ReceiverLinkCreditPolicy#CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING}
     * for a steady-state flow of incoming messages.
     *
     * The link credit is incremented after it has been processed by the target and notified
     * via {@link #acnowledgeMessageProcessingComplete()}. whenever the link sender's link credit drops below
     * the configurable threshold, the link credit is boosted, and a flow frame generated.
     *
     * @param minLinkCreditThreshold: the link credit at which the credit is boosted.
     * @param linkCreditBoost: amount of link credit to boost.
     */
    public void configureSteadyStatePacedByMessageProcessing(long minLinkCreditThreshold, long linkCreditBoost);

    /**
     * Called by target end-point to drop the link credit to 0, so that it stops the incoming message flow.
     * It could still receive in-flight messages from peer.
     */
    public void stop();

    /**
     * Called by target end-point to notify after a message has been processed.
     * Used with the link credit policy
     * {@link CAMQPEndpointPolicy.ReceiverLinkCreditPolicy#CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING}.
     *
     * This enables the LinkReceiver to keep track of how many incoming messages
     * have been processed since the last flow-frame was sent, so that it can boost
     * the link credit.
     */
    public void acnowledgeMessageProcessingComplete();

    /**
     * Called by target end-point to (re)configure link-credit state based on CAMQPEndpointPolicy
     * and send a flow-frame to the peer with the updated link-credit, if needed.
     */
    public void provideLinkCredit();
}
