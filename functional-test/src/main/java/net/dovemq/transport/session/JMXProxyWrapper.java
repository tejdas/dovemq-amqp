package net.dovemq.transport.session;

import java.io.IOException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXProxyWrapper
{
    private final SessionCommandMBean sessionBean;
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
        ObjectName mbeanName = new ObjectName(SessionSysTestJMXServer.SESSION_COMMAND_BEAN);
        sessionBean = JMX.newMBeanProxy(mbsc, mbeanName, SessionCommandMBean.class, true);
    }
    
    void cleanup() throws IOException
    {
        jmxc.close();
    }    
}
