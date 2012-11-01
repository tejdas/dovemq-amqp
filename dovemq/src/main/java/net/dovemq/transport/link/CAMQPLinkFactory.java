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
import net.dovemq.transport.session.CAMQPSessionFactory;
import net.dovemq.transport.session.CAMQPSessionInterface;

/**
 * Factory class to initiate creation of Link Sender and Link Receiver
 * @author tejdas
 */
public final class CAMQPLinkFactory
{
    public static CAMQPLinkSenderInterface createLinkSender(String targetContainerId, String source, String target)
    {
        return createLinkSender(targetContainerId, source, target, new CAMQPEndpointPolicy());
    }

    public static CAMQPLinkSenderInterface createLinkSender(String targetContainerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPSessionInterface session = CAMQPSessionFactory.getOrCreateCAMQPSession(targetContainerId);
        if (session != null)
        {
            CAMQPLinkSender sender = new CAMQPLinkSender(session);
            sender.createLink(source, target, endpointPolicy);
            return sender;
        }
        return null;
    }

    public static CAMQPLinkReceiverInterface createLinkReceiver(String targetContainerId, String source, String target)
    {
        return createLinkReceiver(targetContainerId, source, target, new CAMQPEndpointPolicy());
    }

    public static CAMQPLinkReceiverInterface createLinkReceiver(String targetContainerId, String source, String target, CAMQPEndpointPolicy endpointPolicy)
    {
        CAMQPSessionInterface session = CAMQPSessionFactory.getOrCreateCAMQPSession(targetContainerId);
        if (session != null)
        {
            CAMQPLinkReceiver receiver = new CAMQPLinkReceiver(session);
            receiver.createLink(source, target, endpointPolicy);
            return receiver;
        }
        return null;
    }

    public void linkReceiverCreated()
    {
    }
}
