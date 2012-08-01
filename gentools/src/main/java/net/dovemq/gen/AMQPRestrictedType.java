package net.dovemq.gen;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPRestrictedType implements AMQPType
{
    AMQPRestrictedType(Element xmlElement)
    {
        super();
        this.name = xmlElement.getAttributeValue("name");
        this.type = xmlElement.getAttributeValue("source");
        this.provides = xmlElement.getAttributeValue("provides");        
        this.label = xmlElement.getAttributeValue("label");
        this.choices = new HashMap<String, Object>();
        this.xmlElement = xmlElement;
    }

    void
    generateConstants(PrintWriter outputStream) throws DataConversionException
    {
        String innerType = getType();
        
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>) xmlElement.getChildren();
        for (Element c : children)
        {
            if (c.getName().equalsIgnoreCase("choice"))
            {
                String choiceName = c.getAttributeValue("name");
                if (innerType.equalsIgnoreCase("symbol"))
                {
                    String choiceValue = c.getAttribute("value").getValue();
                    choices.put(choiceName, choiceValue);
                }
                else if (innerType.equalsIgnoreCase("ushort")
                        || innerType.equalsIgnoreCase("ubyte")
                        || innerType.equalsIgnoreCase("uint"))
                {
                    int choiceValue = c.getAttribute("value").getIntValue();
                    choices.put(choiceName, choiceValue);
                }
            }
        }
        
        Collection<Entry<String, Object>> entries = choices.entrySet();
        for (Entry<String, Object> entry : entries)
        {
            String nameUpper = name.toUpperCase().replace("-", "_");
            String keyUpper = entry.getKey().toUpperCase().replace("-", "_");
            outputStream.println();
            outputStream.println(String.format("    public static final String %s_%s_STR = \"%s\";", nameUpper, keyUpper, entry.getKey()));
            if (innerType.equalsIgnoreCase("symbol"))
            {
                outputStream.println(String.format("    public static final String %s_%s = \"%s\";", nameUpper, keyUpper, (String) entry.getValue()));
            }
            else
            {
                outputStream.println(String.format("    public static final int %s_%s = %d;", nameUpper, keyUpper, (Integer) entry.getValue()));
            }
        }        
    }

    @Override
    public void display()
    {
        System.out.println("name: " + name);
        System.out.println("type: " + type);
        System.out.println("label: " + label);
        System.out.println("provides: " + provides);
        Collection<Entry<String, Object>> vals = choices.entrySet();
        for (Entry<String, Object> val : vals)
        {
            System.out.println(val.getKey() + "  " + val.getValue());
        }
    }

    @Override
    public final String getName()
    {
        if (name.equalsIgnoreCase("*"))
        {
            System.out.println(name);
        }
        return name;
    }

    String getType()
    {
        if (type.equalsIgnoreCase("*"))
        {
            System.out.println(type);
        }
        if (AMQPXMLReader.gPrimitiveTypes.contains(type))
        {
            return type;
        }
        else if (AMQPXMLReader.gRestrictedTypes.contains(type))
        {
            AMQPType innerType = AMQPXMLReader.gRestrictedTypes.getType(type);
            AMQPRestrictedType restrictedInnerType = (AMQPRestrictedType) innerType;
            return restrictedInnerType.getType();
        }
        System.out.println("Returning null here for: " + type);
        return null;
    }
    
    private final String name;
    
    private final String provides;    

    @Override
    public String getProvides()
    {
        return provides;
    }

    private final String type;

    private final String label;

    private final Map<String, Object> choices;
    
    private final Element xmlElement;

    @Override
    public String getSource()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
