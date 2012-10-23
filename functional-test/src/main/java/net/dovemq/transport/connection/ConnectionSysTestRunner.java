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
import java.io.OutputStream;

public class ConnectionSysTestRunner
{
    public static void main(String[] args)
    {
        try
        {
            // Execute command
            String command = "java -Dcamqp.testpath=./server.log -Dlog4j.configuration=file:log4j.properties net.dovemq.transport.connection.ConnectionSysTestServer";
            Process child = Runtime.getRuntime().exec(command);

            // Get output stream to write from it
            OutputStream out = child.getOutputStream();

            Thread.sleep(5000);
            String cmd = "shutdown";
            out.write(cmd.getBytes());
            out.close();
        }
        catch (IOException e)
        {
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
