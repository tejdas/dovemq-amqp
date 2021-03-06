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
import java.util.concurrent.atomic.AtomicLong;

import net.dovemq.broker.endpoint.CAMQPMessageReceiver;
import net.dovemq.transport.endpoint.CAMQPTargetInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.session.CAMQPSessionInterface;

public class LinkTestTarget implements CAMQPTargetInterface {
    private final AtomicLong messageCount = new AtomicLong(0);

    @Override
    public void messageReceived(long deliveryId, String deliveryTag, CAMQPMessagePayload message, boolean settledBySender, int receiverSettleMode) {
        messageCount.incrementAndGet();
    }

    public long getNumberOfMessagesReceived() {
        return messageCount.longValue();
    }

    public void resetNumberOfMessagesReceived() {
        messageCount.set(0);
    }

    @Override
    public Collection<Long> processDisposition(Collection<Long> deliveryIds, boolean settleMode, Object newState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerMessageReceiver(CAMQPMessageReceiver targetReceiver) {
        // TODO Auto-generated method stub

    }

    @Override
    public void acknowledgeMessageProcessingComplete(long deliveryId) {
        // TODO Auto-generated method stub
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void closeUnderlyingLink(CAMQPDefinitionError errorDetails) {
        // TODO Auto-generated method stub
    }

    @Override
    public CAMQPSessionInterface getSession() {
        // TODO Auto-generated method stub
        return null;
    }
}
