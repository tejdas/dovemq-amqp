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

package net.dovemq.transport.session;

import java.util.Collection;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.LinkRole;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public class SysTestCommandReceiver extends SysBaseLinkReceiver {
    public SysTestCommandReceiver(CAMQPSessionInterface session) {
        super(session);
    }

    @Override
    public void transferReceived(long transferId,
            CAMQPControlTransfer transferFrame,
            CAMQPMessagePayload payload) {
        super.transferReceived(transferId, transferFrame, payload);
        session.ackTransfer(transferId);
    }

    @Override
    public Collection<Long> dispositionReceived(Collection<Long> deliveryIds,
            boolean settleMode,
            Object newState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkRole getRole() {
        // TODO Auto-generated method stub
        return LinkRole.LinkReceiver;
    }
}
