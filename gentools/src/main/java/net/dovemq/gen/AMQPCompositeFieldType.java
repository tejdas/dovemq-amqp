package net.dovemq.gen;

import java.util.ArrayList;
import java.util.Collection;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

class AMQPCompositeFieldType
{
    public AMQPCompositeFieldType(Element xmlElement)
            throws DataConversionException
    {
        super();
        this.name = xmlElement.getAttributeValue("name");
        this.type = xmlElement.getAttributeValue("type");

        Attribute requiredAttrib = xmlElement.getAttribute("mandatory");
        this.required =
                (requiredAttrib != null) ? requiredAttrib.getValue()
                        .equalsIgnoreCase("true") : false;
        Attribute multipleAttrib = xmlElement.getAttribute("multiple");
        this.multiple =
                (multipleAttrib != null) ? multipleAttrib.getValue()
                         .equalsIgnoreCase("true") : false;                        
        this.label = xmlElement.getAttributeValue("label");
        
        String requires = xmlElement.getAttributeValue("requires");
        this.requiresType = (requires == null)? "" : requires;
        
        defaultValue = xmlElement.getAttributeValue("default");
    }

    void display()
    {
        System.out.println("    name: " + name);
        System.out.println("    type: " + type);
        System.out.println("    required: " + required);
        System.out.println("    multiple: " + multiple);        
        System.out.println("    label: " + label);
    }

    final String getName()
    {
        return name;
    }

    String getType()
    {
        return type;
    }    
    void loadProviders()
    {
        if (type.equalsIgnoreCase("*"))
        {
            AMQPXMLReader.gDefinitionsCompositeTypes.loadAllProvidersFor(requiresType, providers);
            AMQPXMLReader.gRestrictedTypes.loadAllProvidersFor(requiresType, providers);
            if (providers.size() > 0)
            {
                type = "Object";
            }
            else
            {
                System.out.println("ERROR CASE REVISIT tejdas : name: " + name + "  type: " + type);
                type = "string";
            }
        }
    }    

    final boolean isRequired()
    {
        return required;
    }
    
    final boolean isMultiple()
    {
        return multiple;
    }    
    
    final String getDefaultValue()
    {
        return defaultValue;
    }

    private final String name;

    private String type;

    private final boolean required;
    
    private final boolean multiple;

    private final String label;
    
    private final String requiresType;
    
    private final String defaultValue;
    
    private final Collection<String> providers = new ArrayList<String>();

    final Collection<String> getProviders()
    {
        return providers;
    }
}