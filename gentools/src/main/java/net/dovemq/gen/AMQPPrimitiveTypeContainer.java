package net.dovemq.gen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPPrimitiveTypeContainer implements AMQPTypeContainer
{
    AMQPPrimitiveTypeContainer(Element xmlElement)
            throws DataConversionException
    {
        super();
        primitiveTypes = new HashMap<String, AMQPPrimitiveType>();
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>) xmlElement.getChildren();
        for (Element c : children)
        {
            if (c.getName().equalsIgnoreCase("type"))
            {
                AMQPPrimitiveType primitiveType = new AMQPPrimitiveType(c);
                primitiveTypes.put(primitiveType.getName(), primitiveType);
            }
        }
        primitiveTypeNames = primitiveTypes.keySet();
    }

    @Override
    public void display()
    {
        Collection<AMQPPrimitiveType> vals = primitiveTypes.values();
        for (AMQPPrimitiveType val : vals)
        {
            System.out.println("-------------------------------------");
            val.display();
            System.out.println();
        }
    }

    @Override
    public boolean
    contains(String name)
    {
        return primitiveTypeNames.contains(name);
    }

    @Override
    public AMQPType
    getType(String name)
    {
        return primitiveTypes.get(name);
    }

    private final Map<String, AMQPPrimitiveType> primitiveTypes;

    private final Collection<String> primitiveTypeNames;
}
