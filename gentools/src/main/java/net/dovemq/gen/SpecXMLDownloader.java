package net.dovemq.gen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class SpecXMLDownloader 
{
	public static final String[] xmlFileNames = {"amqp.dtd", "index.xml", "types.xml", "transport.xml", "messaging.xml", "security.xml", "transactions.xml"};
	private static String revision = null;
    public static void main( String[] args ) throws IOException
    {
    	if (args.length > 0)
    		revision = args[0];
    	
    	for (String fileName : xmlFileNames)
    	{
            URL amqpXmlUrl = new URL(String.format("http://www.amqp.org/viewvc/trunk/specification/%s?revision=%s", fileName, revision));
            URLConnection urlConnection = amqpXmlUrl.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            
            BufferedWriter out = new BufferedWriter(new FileWriter(String.format("%s/camqp/spec/%s", System.getenv("HOME"), fileName)));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
            {
            	out.write(inputLine);
            	out.newLine();
            }
            in.close();
            out.close();
    	}
    }
}
