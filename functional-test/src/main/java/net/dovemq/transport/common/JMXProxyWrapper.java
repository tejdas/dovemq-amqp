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
