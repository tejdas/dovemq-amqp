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

package net.dovemq.transport.common;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.dovemq.transport.connection.ConnectionCommand;
import net.dovemq.transport.connection.ConnectionCommandMBean;
import net.dovemq.transport.link.CAMQPLinkManager;
import net.dovemq.transport.link.CAMQPLinkManager.LinkSenderType;
import net.dovemq.transport.link.LinkCommand;
import net.dovemq.transport.link.LinkCommandMBean;
import net.dovemq.transport.session.SessionCommand;
import net.dovemq.transport.session.SessionCommandMBean;

public class DoveMQTestJMXServer {
    public static void main(String[] args) throws InterruptedException,
            IOException {
        CAMQPLinkManager.setLinkSenderType(LinkSenderType.PULL);
        CAMQPLinkManager.initialize(true, "broker");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ConnectionCommandMBean connectionCommandBean = new ConnectionCommand();
        registerBean(mbs,
                connectionCommandBean,
                JMXConstants.CONNECTION_COMMAND_BEAN);

        SessionCommandMBean sessionCommandBean = new SessionCommand();
        registerBean(mbs, sessionCommandBean, JMXConstants.SESSION_COMMAND_BEAN);

        LinkCommandMBean linkCommandBean = new LinkCommand();
        registerBean(mbs, linkCommandBean, JMXConstants.LINK_COMMAND_BEAN);

        ConnectionCommand beanImpl = (ConnectionCommand) connectionCommandBean;

        while (!beanImpl.isShutdown()) {
            Thread.sleep(1000);
        }

        CAMQPLinkManager.shutdown();
    }

    private static void registerBean(MBeanServer mbs,
            Object commandBean,
            String beanName) {
        try {
            ObjectName commandBeanName = new ObjectName(beanName);
            mbs.registerMBean(commandBean, commandBeanName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
