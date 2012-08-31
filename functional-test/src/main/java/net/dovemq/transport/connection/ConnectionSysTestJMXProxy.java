package net.dovemq.transport.connection;

import java.io.IOException;
import java.util.Arrays;

import javax.management.MalformedObjectNameException;

import net.dovemq.transport.common.JMXProxyWrapper;

public class ConnectionSysTestJMXProxy
{
    public static void main(String[] args) throws IOException, MalformedObjectNameException, NullPointerException
    {
        if (args.length == 0)
            return;
        
        JMXProxyWrapper jmxWrapper = new JMXProxyWrapper(args[0], args[1]);        

        ConnectionCommandMBean mbeanProxy = jmxWrapper.getConnectionBean();
        
        ConnectionSysTestCmdDriver.setCommandExecutor(mbeanProxy);

        String[] cmdArgs = Arrays.copyOfRange(args, 2, args.length);
        String cmd = cmdArgs[0];
        ConnectionSysTestCmdDriver.processCommand(cmd, cmdArgs, false);
        
        jmxWrapper.cleanup();
    }
}
