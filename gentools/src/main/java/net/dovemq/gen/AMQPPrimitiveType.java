package net.dovemq.gen;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPPrimitiveType implements AMQPType
{
    final boolean isHasMultipleTypeVals()
    {
        return hasMultipleTypeVals;
    }

    @Override
    public void display()
    {
        System.out.println("name: " + name);
        System.out.println("type: " + type);        
        System.out.println("label: " + label);
        System.out.println("hasMultipleTypeVals: " + hasMultipleTypeVals);
        for (AMQPPrimitiveTypeDetails val : typeValues)
        {
            val.display();
            System.out.println();
        }
    }

    @Override
    public final String getName()
    {
        return name;
    }
    
    final String getType()
    {
        return type;
    }
    
    void generateFormatcodeConstants(PrintWriter outputStream)
    {
        for (AMQPPrimitiveTypeDetails detail : typeValues)
        {
            String nameToChoose = (hasMultipleTypeVals) ? detail.getName() : name;
            String nameToPrint = nameToChoose.replace('-', '_').toUpperCase();
            String decl = String.format("public static final int %s = 0x%x;", nameToPrint, detail.getCode());
            outputStream.println(Utils.insertTabs(1, decl));
        }
    }
    
    void generateAmqpTypeEnums(PrintWriter outputStream)
    {
        for (AMQPPrimitiveTypeDetails detail : typeValues)
        {
            String nameToChoose = (hasMultipleTypeVals) ? detail.getName() : name;
            String nameToPrint = nameToChoose.replace('-', '_').toUpperCase();
            String enumType = String.format("%s (CAMQPFormatCodes.%s, \"%s\", %s),", nameToPrint, nameToPrint, nameToPrint, getWidth(detail.getWidth()));
            outputStream.println(Utils.insertTabs(1, enumType));
        }
    }
    
    private static String getWidth(int width)
    {
        switch (width)
        {
            case 0:
                return "Width.FIXED_ZERO";
            case 1:
                return "Width.FIXED_ONE";
            case 2:
                return "Width.FIXED_TWO";
            case 4:
                return "Width.FIXED_FOUR";
            case 8:
                return "Width.FIXED_EIGHT";
            case 16:
                return "Width.FIXED_SIXTEEN";
        }
        return null;
    }

    AMQPPrimitiveType(Element xmlElement) throws DataConversionException
    {
        super();
        typeValues = new ArrayList<AMQPPrimitiveTypeDetails>();
        this.name = xmlElement.getAttributeValue("name");
        this.type = AMQPPrimitiveTypeMappings.getJavaType(this.name);
        this.label = xmlElement.getAttributeValue("label");
       
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>) xmlElement.getChildren();
        this.hasMultipleTypeVals = (children.size() > 1);
        for (Element c : children)
        {
            if (c.getName().equalsIgnoreCase("encoding"))
            {
                AMQPPrimitiveTypeDetails val =
                        new AMQPPrimitiveTypeDetails(this.name, c);
                typeValues.add(val);
            }
        }
    }

    private final boolean hasMultipleTypeVals;

    private final String name;
    
    private final String type;    

    private final String label;
    
    private final List<AMQPPrimitiveTypeDetails> typeValues;

    @Override
    public String getProvides()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSource()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
