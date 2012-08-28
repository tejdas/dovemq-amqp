package net.dovemq.transport.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConnectionSysTestCmdDriver
{
    private static ConnectionCommandMBean commandExecutor = null;
    static void setCommandExecutor(ConnectionCommandMBean commandExecutor)
    {
        ConnectionSysTestCmdDriver.commandExecutor = commandExecutor;
    }
    
    private static ConnectionCommandMBean getCommandExecutor()
    {
        if (ConnectionSysTestCmdDriver.commandExecutor == null)
        {
            ConnectionSysTestCmdDriver.commandExecutor = new ConnectionCommand();
        }
        return ConnectionSysTestCmdDriver.commandExecutor;
    }
    
    public static boolean processCommand(String cmd, String[] argList, boolean isServer)
    {
        if (cmd.equalsIgnoreCase("help"))
        {
            getCommandExecutor().help();
            return true;
        }
        else if (cmd.equalsIgnoreCase("create") && !isServer)
        {
            getCommandExecutor().create(argList[1]);
            return true;
        }
        else if (cmd.equalsIgnoreCase("create") && isServer)
        {
            System.out.println("create not supported");
            return true;
        }
        else if (cmd.equalsIgnoreCase("shutdown"))
        {
            getCommandExecutor().shutdown();
            return false;
        }
        else if (cmd.equalsIgnoreCase("list"))
        {
            getCommandExecutor().list();
            return true;
        }
        else if (cmd.equalsIgnoreCase("close"))
        {
            getCommandExecutor().close(argList[1]);
            return true;
        }
        else if (cmd.equalsIgnoreCase("closeAsync"))
        {
            getCommandExecutor().closeAsync(argList[1]);
            return true;
        }
        else if (cmd.equalsIgnoreCase("isClosed"))
        {
            if (getCommandExecutor().checkClosed(argList[1]))
                System.out.println("Connection to container: " + argList[1] + " already closed");
            else
                System.out.println("Connection to container: " + argList[1] + " not closed yet");
            return true;
        }
        else
        {
            getCommandExecutor().help();
            return true;
        }
    }

    protected static void processConsoleInput(boolean isServer) throws IOException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true)
        {
            System.out.println("Choose an option");
            String s = in.readLine();
            if ((s != null) && s.length() != 0)
            {
                String[] argList = null;
                String cmd = null;
                if (s.indexOf(" ") != -1)
                {
                    argList = s.split(" ");
                    cmd = argList[0];
                }
                else
                {
                    cmd = s;
                }
                if (!processCommand(cmd, argList, isServer))
                {
                    break;
                }
            }
        }
    }
}
