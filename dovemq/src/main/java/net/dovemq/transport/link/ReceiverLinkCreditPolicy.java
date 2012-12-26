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

/**
 * ReceiverLinkCreditPolicy determines how the Link credit
 * is increased when it drops to (below) zero, or approaches
 * the minimum threshold.
 *
 * @author tejdas
 */
public enum ReceiverLinkCreditPolicy {
    /*
     * Link credit is offered by the target receiver, whenever
     * it wants to get message(s).
     */
    CREDIT_OFFERED_BY_TARGET,
    /*
     * When the Link receiver's computed link credit goes below
     * a certain threshold, it automatically increases the link
     * credit. This enables messages to flow at a steady state,
     * and the Link sender never runs out of link-credit.
     */
    CREDIT_STEADY_STATE,
    /*
     * Link credit is incremented whenever Link sender has no
     * more credit, and sends a flow-frame to ask for more credit.
     */
    CREDIT_AS_DEMANDED_BY_SENDER,
    /*
     * Link credit is incremented every-time target acknowledges completion
     * of processing a message. A flow-frame is sent whenever it is asked by
     * the sender.
     */
    CREDIT_STEADY_STATE_DRIVEN_BY_TARGET_MESSAGE_PROCESSING
}