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
import java.util.Random;

import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.LinkRole;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public class SysDelayedAckLinkReceiver extends SysBaseLinkReceiver implements
        Runnable {
    public long lastTransferIdReceived = 0;

    public long lastTransferIdAcked = 0;

    public volatile boolean shutdown = false;

    public SysDelayedAckLinkReceiver(CAMQPSessionInterface session) {
        super(session);
    }

    @Override
    public void transferReceived(long transferId,
            CAMQPControlTransfer transferFrame,
            CAMQPMessagePayload payload) {
        synchronized (this) {
            lastTransferIdReceived = transferId;
        }

        super.transferReceived(transferId, transferFrame, payload);
    }

    @Override
    public void sessionClosed() {
        super.sessionClosed();
        shutdown = true;
    }

    void ackTransfers(long startId, long endId) {
        for (long i = startId; i < endId; i++) {
            session.ackTransfer(i);
        }
    }

    @Override
    public void run() {
        Random r = new Random();
        boolean firstTime = true;
        while (!shutdown) {
            try {
                Thread.sleep(r.nextInt(3000) + 5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long lastReceived;
            long ackStart;
            synchronized (this) {
                if (lastTransferIdAcked == lastTransferIdReceived)
                    continue;
                lastReceived = lastTransferIdReceived;
                ackStart = (firstTime) ? 0 : lastTransferIdAcked + 1;
            }

            if (firstTime)
                firstTime = false;

            System.out.println("timer acking from : " + ackStart
                    + " to "
                    + lastReceived);
            for (long i = ackStart; i <= lastReceived; i++) {
                session.ackTransfer(i);
            }

            synchronized (this) {
                lastTransferIdAcked = lastReceived;
            }
        }
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
