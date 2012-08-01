package net.dovemq.gen;

import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPPrimitiveTypeDetails
{
    final String getName()
    {
        return name;
    }

    final int getCode()
    {
        return code;
    }

    final String getCategory()
    {
        return category;
    }

    final int getWidth()
    {
        return width;
    }

    final String getLabel()
    {
        return label;
    }

    AMQPPrimitiveTypeDetails(String parentName, Element xmlElement)
            throws DataConversionException
    {
        super();
        String elemName = xmlElement.getAttributeValue("name");
        this.name = (elemName != null) ? elemName : parentName;
        String codeAsString = xmlElement.getAttributeValue("code").substring(2);
        this.code = Integer.parseInt(codeAsString, 16);
        this.width = xmlElement.getAttribute("width").getIntValue();
        this.category = xmlElement.getAttributeValue("category");
        this.label = xmlElement.getAttributeValue("label");
    }

    void display()
    {
        System.out.println("    name: " + name);
        System.out.println("    code: " + code);
        System.out.println("    category: " + category);
        System.out.println("    width: " + width);
        System.out.println("    label: " + label);
    }

    private final String name;

    private final int code;

    private final String category;

    private final int width;

    private final String label;
}
