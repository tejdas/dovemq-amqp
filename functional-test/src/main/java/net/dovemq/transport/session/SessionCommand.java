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

import java.io.IOException;
import java.util.Collection;

public class SessionCommand implements SessionCommandMBean {
    @Override
    public void registerFactory(String factoryName) {
        SysTestCommandReceiverFactory commandReceiverFactory = new SysTestCommandReceiverFactory(factoryName);
        CAMQPSessionManager.registerLinkReceiverFactory(commandReceiverFactory);
        System.out.println("Registered factory: " + factoryName);
    }

    @Override
    public void sessionCreate(String targetContainerId) {
        CAMQPSessionFactory.createCAMQPSession(targetContainerId);
    }

    @Override
    public void sessionCreateMT(String targetContainerId, int numThreads) {
        SessionCommand command = new SessionCommand();
        try {
            SessionIOTestUtils.createSessions(numThreads,
                    targetContainerId,
                    command);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Collection<Integer> getChannelId(String targetContainerId) {
        Collection<Integer> sessionList = CAMQPSessionManager.getAllAttachedChannels(targetContainerId);
        return sessionList;
    }

    @Override
    public void sessionIO(String targetContainerId, String dest) {
        Collection<Integer> sessionList = CAMQPSessionManager.getAllAttachedChannels(targetContainerId);
        for (int channelId : sessionList) {
            CAMQPSession session = CAMQPSessionManager.getSession(targetContainerId,
                    channelId);
            if (session == null) {
                System.out.println("Null session for channel: " + channelId);
                return;
            }

            String destfile = String.format("%s.%d", dest, channelId);
            try {
                SessionIOTestUtils.transmitBinaryData(session, destfile);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sessionClose(String targetContainerId,
            int sessionAttachedChannelId) {
        CAMQPSession session = CAMQPSessionManager.getSession(targetContainerId,
                sessionAttachedChannelId);
        if (session != null)
            session.close();
        else
            System.out.println("Session with channelId: " + sessionAttachedChannelId
                    + " to container: "
                    + targetContainerId
                    + " is already closed");
    }

    @Override
    public boolean isIODone() {
        SysTestCommandReceiverFactory commandReceiverFactory = (SysTestCommandReceiverFactory) CAMQPSessionManager.getLinkReceiverFactory();
        return commandReceiverFactory.isDone();
    }

    @Override
    public void setSessionWindowSize(long maxOutgoingWindowSize,
            long maxIncomingWindowSize) {
        CAMQPSessionManager.setMaxSessionWindowSize(maxOutgoingWindowSize,
                maxIncomingWindowSize);
    }
}
