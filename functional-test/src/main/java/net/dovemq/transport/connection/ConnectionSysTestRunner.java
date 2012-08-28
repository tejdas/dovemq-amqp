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
