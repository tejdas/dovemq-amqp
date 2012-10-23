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
