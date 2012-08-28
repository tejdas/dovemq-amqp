package net.dovemq.transport.connection;

import java.io.IOException;
import java.util.Arrays;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ConnectionSysTestJMXProxy
{
    public static void main(String[] args) throws IOException, MalformedObjectNameException, NullPointerException
    {
        if (args.length == 0)
            return;
        
        JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://:%s/jmxrmi", args[0]));
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName mbeanName = new ObjectName("CAMQP:name=ConnectionCommandMBean");

        ConnectionCommandMBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, ConnectionCommandMBean.class, true);
        
        ConnectionSysTestCmdDriver.setCommandExecutor(mbeanProxy);

        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
        String cmd = cmdArgs[0];
        ConnectionSysTestCmdDriver.processCommand(cmd, cmdArgs, false);
        
        jmxc.close();
    }
}
