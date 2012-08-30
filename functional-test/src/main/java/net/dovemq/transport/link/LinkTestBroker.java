package net.dovemq.transport.link;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LinkTestBroker
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        CAMQPLinkManager.initialize(true, "broker");
        
        System.out.println("Press any key to continue");
      
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        while ((s = in.readLine()) != null && s.length() != 0)
        {
            if (s.equalsIgnoreCase("done"))
                break;
        }
        
        System.out.println("shutting down");
        CAMQPLinkManager.shutdown();
    }
}
