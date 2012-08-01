package net.dovemq.gen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPRestrictedTypeContainer implements AMQPTypeContainer
{
    AMQPRestrictedTypeContainer(Element xmlElement)
            throws DataConversionException
    {
        super();
        restrictedTypes = new HashMap<String, AMQPRestrictedType>();
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>) xmlElement.getChildren();
        for (Element c : children)
        {
            if (c.getName().equalsIgnoreCase("type")
                    && c.getAttributeValue("class").equalsIgnoreCase(
                            "restricted"))
            {
                AMQPRestrictedType restrictedType = new AMQPRestrictedType(c);
                restrictedTypes.put(restrictedType.getName(), restrictedType);
            }
        }
        restrictedTypeNames = restrictedTypes.keySet();
    }

    @Override
    public void
    display()
    {
        Collection<AMQPRestrictedType> vals = restrictedTypes.values();
        for (AMQPRestrictedType val : vals)
        {
            System.out.println("-------------------------------------");
            val.display();
            System.out.println();
        }
    }
    
    void
    generateConstants(String dir) throws IOException, DataConversionException
    {
        Collection<AMQPRestrictedType> vals = restrictedTypes.values();
        String fileName = String.format("%s/%s.java", dir, "CAMQPConstants");
        PrintWriter outputStream = new PrintWriter(new FileWriter(fileName, false));
        try
        {
            outputStream.println(String.format("package %s;", AMQPXMLReader.PACKAGE_NAME));
            outputStream.println();
            outputStream.println("public class CAMQPConstants");
            outputStream.println("{");            
            for (AMQPRestrictedType val : vals)
            {
                val.generateConstants(outputStream);
                outputStream.println();
            }
            outputStream.println("}");                  
        }
        finally
        {
            outputStream.close();
        }
    }

    @Override
    public boolean
    contains(String name)
    {
        return restrictedTypeNames.contains(name);
    }

    @Override
    public AMQPRestrictedType
    getType(String name)
    {
        return restrictedTypes.get(name);
    }

    private final Map<String, AMQPRestrictedType> restrictedTypes;

    private final Collection<String> restrictedTypeNames;
}
