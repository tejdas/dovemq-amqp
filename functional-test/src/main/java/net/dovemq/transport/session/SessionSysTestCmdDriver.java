package net.dovemq.transport.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import net.dovemq.transport.connection.ConnectionSysTestCmdDriver;
import net.dovemq.transport.link.CAMQPLinkMessageHandlerFactory;

public class SessionSysTestCmdDriver
{
    protected static boolean
    processCommand(String cmd, String[] argList, boolean isServer)
    {
        if (cmd.equalsIgnoreCase("sessionHelp"))
        {
            System.out.println("registerFactory");
            System.out.println("getRegisteredFactory");
            System.out.println("getFactoryList");            
            System.out.println("sessionList [targetContainerId]");
            System.out.println("sessionCreate [targetContainerId]");
            System.out.println("sessionCreateMT [targetContainerId] [numThreads]");
            System.out.println("sessionIO [targetContainerId] [sessionAttachedChannelId] [source] [dest]");            
            System.out.println("sessionClose [targetContainerId] [sessionAttachedChannelId]");
            return true;
        }
        else if (cmd.equalsIgnoreCase("registerFactory"))
        {
            if ((argList == null) || (argList.length < 2))
            {
                return true;
            }
            SysTestCommandReceiverFactory commandReceiverFactory = new SysTestCommandReceiverFactory(argList[1]);        
            CAMQPSessionManager.registerLinkReceiverFactory(commandReceiverFactory);
            return true;
        }        
        else if (cmd.equalsIgnoreCase("getFactoryList"))
        {
            Collection<String> list = SysTestCommandReceiverFactory.getAvailableFactoryList();
            for (String s: list)
            {
                System.out.println(s);
            }
            return true;
        }        
        else if (cmd.equalsIgnoreCase("getRegisteredFactory"))
        {
            CAMQPLinkMessageHandlerFactory factory = CAMQPSessionManager.getLinkReceiverFactory();
            if (factory == null)
            {
                System.out.println("No CAMQPCommandReceiverFactory registered");
            }
            else
            {
                SysTestCommandReceiverFactory commandReceiverFactory = (SysTestCommandReceiverFactory) factory;
                System.out.println(commandReceiverFactory.getCommandReceiverClassName());
            }
            return true;
        }        
        else if (cmd.equalsIgnoreCase("sessionCreate"))
        {
            if ((argList == null) || (argList.length < 2))                
            {
                return true;
            }            
            CAMQPSessionFactory.createCAMQPSession(argList[1]);
            System.out.println("Session opened");
            return true;
        }       
        else if (cmd.equalsIgnoreCase("sessionCreateMT"))
        {
            return true;
        }        
        else if (cmd.equalsIgnoreCase("sessionClose"))
        {
            if ((argList == null) || (argList.length < 3))
            {
                return true;
            }            
            String targetContainerId = argList[1];
            int sessionAttachedChannelId = Integer.parseInt(argList[2]);
            CAMQPSession session = CAMQPSessionManager.getSession(targetContainerId, sessionAttachedChannelId);
            if (session != null)
                session.close();
            else
                System.out.println("Session with channelId: " +  sessionAttachedChannelId + " to container: " + targetContainerId + " is already closed");
            return true;
        }
        else if (cmd.equalsIgnoreCase("sessionList"))
        {
            if ((argList == null) || (argList.length < 2))
            {
                return true;
            }            
            String targetContainerId = argList[1];
            Collection<Integer> sessionList = CAMQPSessionManager.getAllAttachedChannels(targetContainerId);
            for (int i : sessionList)
            {
                System.out.println("Session outgoingChannelId: " + i); 
            }
            return true;
        }
        else if (cmd.equalsIgnoreCase("sessionIO"))
        {
            if ((argList == null) || (argList.length < 5))
            {
                return true;
            }            
            String targetContainerId = argList[1];
            int sessionAttachedChannelId = Integer.parseInt(argList[2]);
  
            try
            {
                CAMQPSession session = CAMQPSessionManager.getSession(targetContainerId, sessionAttachedChannelId);
                if (session != null)
                    SessionIOTestUtils.transmitFile(session, argList[3], argList[4]);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }        
        else if (cmd.equalsIgnoreCase("sessionCloseAll"))
        {
            return true;
        }        
        else
        {
            return ConnectionSysTestCmdDriver.processCommand(cmd, argList, isServer);
        }
    }

    protected static void
    processConsoleInput(boolean isServer) throws IOException, CAMQPSessionBeginException
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
