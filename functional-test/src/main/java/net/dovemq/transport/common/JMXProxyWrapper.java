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

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import net.dovemq.transport.link.LinkCommandMBean;
import net.dovemq.transport.session.SessionCommandMBean;
import net.dovemq.transport.connection.ConnectionCommandMBean;

public class JMXProxyWrapper
{
    private final SessionCommandMBean sessionBean;
    private final ConnectionCommandMBean connectionBean;
    private final LinkCommandMBean linkBean;
    public LinkCommandMBean getLinkBean()
    {
        return linkBean;
    }

    public ConnectionCommandMBean getConnectionBean()
    {
        return connectionBean;
    }

    public SessionCommandMBean getSessionBean()
    {
        return sessionBean;
    }

    private final JMXConnector jmxc;
    
    public JMXProxyWrapper(String remoteIp, String jmxPort) throws IOException, MalformedObjectNameException
    {
        JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", remoteIp, jmxPort));
        jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        ObjectName mbeanName = new ObjectName(JMXConstants.SESSION_COMMAND_BEAN);
        sessionBean = JMX.newMBeanProxy(mbsc, mbeanName, SessionCommandMBean.class, true);
        
        ObjectName mbeanName2 = new ObjectName(JMXConstants.CONNECTION_COMMAND_BEAN);
        connectionBean = JMX.newMBeanProxy(mbsc, mbeanName2, ConnectionCommandMBean.class, true);  

        ObjectName mbeanName3 = new ObjectName(JMXConstants.LINK_COMMAND_BEAN);
        linkBean = JMX.newMBeanProxy(mbsc, mbeanName3, LinkCommandMBean.class, true);  
    }
    
    public void cleanup() throws IOException
    {
        jmxc.close();
    }    
}
