package net.dovemq.gen;

import java.io.IOException;
import java.util.Collection;

import org.jdom.DataConversionException;
import org.jdom.Element;

public class AMQPListCompositeType extends AMQPCompositeType
{
    static enum ProviderType
    {
        UNKNOWN,
        COMPOUND,
        RESTRICTED
    }
    
    AMQPListCompositeType(Element xmlElement, CompoundMetaType metaType) throws DataConversionException
    {
        super(xmlElement, metaType);
    }
    
    @Override
    void
    generateClassFile(String dir) throws IOException
    {
        super.generateClassFileForCompositeType(dir);
    }

    @Override
    protected void
    generateEncoder()
    {
        int numberOfTabs = 1;
        generateEncoderSignature();        
        
        for (AMQPCompositeFieldType field : fields)
        {
            String canonicalFieldName = Utils.convertToCanonicalName(field.getName());
            String fieldName = Utils.lowerCapFirstLetter(canonicalFieldName);
            String requiredBooleanFieldName = String.format("isSet%s", canonicalFieldName);
            
            boolean isRequiredFlag = field.isRequired();
            boolean hasMultiples = field.isMultiple();
            
            if (field.getType().equalsIgnoreCase("Object"))
            {
                if (hasMultiples)
                {
                    System.out.println("requires field has multiple instances: REVISIT TODO");                        
                }
                
                Collection<String> providers = field.getProviders();
                ProviderType providerType = getProviderType(providers);
                if (providerType == ProviderType.COMPOUND)
                {
                    boolean isCompositeTypeList = true;
                    AMQPCompositeCodecUtils.generateEncoderForCompositeTypesMultipleProviders(outputStream, numberOfTabs+1, providers, fieldName, null, requiredBooleanFieldName, isRequiredFlag, isCompositeTypeList); 
                }
                else if (providerType == ProviderType.RESTRICTED)
                {
                    boolean isCompositeTypeList = true;
                    AMQPCompositeCodecUtils.generateEncoderForRestrictedTypesMultipleProviders(outputStream, numberOfTabs+1, providers, fieldName, null, requiredBooleanFieldName, isRequiredFlag, isCompositeTypeList);                    
                }
            }
            else if (AMQPXMLReader.gPrimitiveTypes.contains(field.getType()))
            {
                boolean isCompositeTypeList = true;
                AMQPCompositeCodecUtils.generateEncoderForPrimitiveTypes(outputStream, numberOfTabs+1, field.getType(), fieldName, null, requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);                
            }
            else if (AMQPXMLReader.gRestrictedTypes.contains(field.getType()))
            {
                AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(field.getType());
                String primTypeName = restrictedType.getType();
                boolean isCompositeTypeList = true;
                AMQPCompositeCodecUtils.generateEncoderForPrimitiveTypes(outputStream, numberOfTabs+1, primTypeName, fieldName, null, requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);                
            }
            else if (AMQPXMLReader.gDefinitionsCompositeTypes.contains(field.getType()))
            {
                boolean isCompositeTypeList = true;
                AMQPCompositeCodecUtils.generateEncoderForCompositeTypes(outputStream, numberOfTabs+1, field.getType(), fieldName, null, requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);                
            }          
            else
            {
                System.out.println("Could not generate for encode() : BEGIN");
                field.display();
                System.out.println("Could not generate for encode() : END");
            }
        }
        
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "encoder.fillCompoundSize(listSize);")); // TODO
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));        
    }

    @Override
    protected void
    generateEncoderSignature()
    {
        int numberOfTabs = 1;        
        outputStream.println(Utils.insertTabs(numberOfTabs, String.format("public static void encode(CAMQPEncoder encoder, %s data)", type)));
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        
        long listSize = fields.size();
        
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("long listSize = %s;", String.valueOf(listSize))));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "encoder.writeListDescriptor(descriptor, listSize);"));            
    }
   
    @Override
    protected void
    generateDecoder()
    {
        int numberOfTabs = 1;
        generateDecoderSignature();
        
        for (AMQPCompositeFieldType field : fields)
        {
            outputStream.println();            
            String canonicalFieldName = Utils.convertToCanonicalName(field.getName());
            String fieldName = Utils.lowerCapFirstLetter(canonicalFieldName);

            if (field.getType().equalsIgnoreCase("Object"))
            {
                if (field.isMultiple())
                {
                    System.out.println("requires field has multiple instances: REVISIT TODO");                        
                }

                Collection<String> providers = field.getProviders();
                ProviderType providerType = getProviderType(providers);
                if (providerType == ProviderType.COMPOUND)
                    AMQPCompositeCodecUtils.generateDecoderForCompositeTypesMultipleProviders(outputStream, numberOfTabs+1, providers, fieldName);  
                else if (providerType == ProviderType.RESTRICTED)
                    AMQPCompositeCodecUtils.generateDecoderForRestrictedTypesMultipleProviders(outputStream, numberOfTabs+1, providers, fieldName);
            }            
            else if (AMQPXMLReader.gPrimitiveTypes.contains(field.getType()))
            {
                AMQPCompositeCodecUtils.generateDecoderForPrimitiveTypes(outputStream, numberOfTabs+1, field.getType(), fieldName, field.isMultiple(), field.isRequired());
            }
            else if (AMQPXMLReader.gRestrictedTypes.contains(field.getType()))
            {
                AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(field.getType());
                String primTypeName = restrictedType.getType();
                AMQPCompositeCodecUtils.generateDecoderForPrimitiveTypes(outputStream, numberOfTabs+1, primTypeName, fieldName, field.isMultiple(), field.isRequired());  
            }
            else if (AMQPXMLReader.gDefinitionsCompositeTypes.contains(field.getType()))
            {
                AMQPCompositeCodecUtils.generateDecoderForCompositeTypes(outputStream, numberOfTabs+1, field.getType(), fieldName, field.isMultiple(), field.isRequired());                
            }           
        }
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "return data;"));        
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));        
    }

    @Override
    protected void
    generateDecoderSignature()
    {
        int numberOfTabs = 1;
        outputStream.println(Utils.insertTabs(numberOfTabs, String.format("public static %s decode(CAMQPSyncDecoder decoder)", type)));
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "int formatCode;"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));"));
        outputStream.println();
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "long listSize = decoder.readCompoundSize(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("assert(listSize == %s);", fields.size())));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("%s data = new %s();", type, type)));        
    }    
    
    private ProviderType getProviderType(Collection<String> providers)
    {
        for (String provider : providers)
        {
            if (!AMQPXMLReader.gDefinitionsCompositeTypes.contains(provider))
            {
                if (AMQPXMLReader.gRestrictedTypes.contains(provider))
                {
                    return ProviderType.RESTRICTED;
                }
            }
            else
            {
                return ProviderType.COMPOUND;
            }
        }

        System.out.println("Provider neither a Composite type nor a Restricted type : REVISIT TODO :");
        return ProviderType.UNKNOWN;
    }
}
