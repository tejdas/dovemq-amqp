package net.dovemq.gen;

import java.io.IOException;
import java.util.Collection;

import org.jdom.DataConversionException;
import org.jdom.Element;

public class AMQPMapCompositeType extends AMQPCompositeType
{
    AMQPMapCompositeType(Element xmlElement, CompoundMetaType metaType) throws DataConversionException
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
        generateEncoderSignature();        
        
        for (AMQPCompositeFieldType field : fields)
        {
            String hungarianFieldName = Utils.convertToCanonicalName(field.getName());
            String canonicalFieldName = Utils.lowerCapFirstLetter(hungarianFieldName);
            String requiredBooleanFieldName = String.format("isSet%s", hungarianFieldName);
            
            boolean isRequiredFlag = field.isRequired();
            boolean hasMultiples = field.isMultiple();
        
            int numberOfTabs = 2;
            
            if (field.getType().equalsIgnoreCase("Object"))
            {
                Collection<String> providers = field.getProviders();
                for (String provider : providers)
                {
                    if (!AMQPXMLReader.gDefinitionsCompositeTypes.contains(provider))
                    {
                        System.out.println("Provider is not a Composite type : REVISIT TODO");                        
                    }                    
                }
                
                if (hasMultiples)
                {
                    System.out.println("requires field has multiple instances: REVISIT TODO");                        
                }
                boolean isCompositeTypeList = false;
                AMQPCompositeCodecUtils.generateEncoderForCompositeTypesMultipleProviders(outputStream, numberOfTabs, providers, canonicalFieldName, field.getName(), requiredBooleanFieldName, isRequiredFlag, isCompositeTypeList);                
            }            
            else if (AMQPXMLReader.gPrimitiveTypes.contains(field.getType()))
            {
                boolean isCompositeTypeList = false;
                AMQPCompositeCodecUtils.generateEncoderForPrimitiveTypes(outputStream, numberOfTabs, field.getType(), canonicalFieldName, field.getName(), requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);
            }
            else if (AMQPXMLReader.gRestrictedTypes.contains(field.getType()))
            {
                AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(field.getType());
                String primTypeName = restrictedType.getType();
                boolean isCompositeTypeList = false;
                AMQPCompositeCodecUtils.generateEncoderForPrimitiveTypes(outputStream, numberOfTabs, primTypeName, canonicalFieldName, field.getName(), requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);
            }
            else if (AMQPXMLReader.gDefinitionsCompositeTypes.contains(field.getType()))
            {
                boolean isCompositeTypeList = false;
                AMQPCompositeCodecUtils.generateEncoderForCompositeTypes(outputStream, numberOfTabs, field.getType(), canonicalFieldName, field.getName(), requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);                
            }          
        }
        
        outputStream.println(Utils.insertTabs(2, "encoder.fillCompoundSize(mapSize);")); // TODO        
        outputStream.println(Utils.insertTabs(1, "}"));        
    }

    @Override
    protected void
    generateEncoderSignature()
    {
        outputStream.println(Utils.insertTabs(1, "public static void"));
        outputStream.println(Utils.insertTabs(1, String.format("encode(CAMQPEncoder encoder, %s data)", type)));
        outputStream.println(Utils.insertTabs(1, "{"));

        outputStream.println(Utils.insertTabs(2, "long mapSize = 0;"));
        generateMapSize();
        
        outputStream.println(Utils.insertTabs(2, "encoder.writeMapDescriptor(descriptor, mapSize);"));            
    }
   
    @Override
    protected void
    generateDecoder()
    {
        generateDecoderSignature();
        int numberOfTabs = 2;

        outputStream.println(Utils.insertTabs(numberOfTabs, "for (long i = 0; i < mapSize; i++)"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "String key = null;"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "if (formatCode == CAMQPFormatCodes.SYM8)"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "key = decoder.readString(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
            
        boolean firstElem = true;
        for (AMQPCompositeFieldType field : fields)
        {
            if (firstElem)
            {
                firstElem = false;
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (key.equalsIgnoreCase(\"%s\"))", field.getName())));
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("else if (key.equalsIgnoreCase(\"%s\"))", field.getName())));                
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
            String hungarianFieldName = Utils.convertToCanonicalName(field.getName());
            String canonicalFieldName = Utils.lowerCapFirstLetter(hungarianFieldName);
            
            if (field.getType().equalsIgnoreCase("Object"))
            {
                Collection<String> providers = field.getProviders();
                for (String provider : providers)
                {
                    if (!AMQPXMLReader.gDefinitionsCompositeTypes.contains(provider))
                    {
                        System.out.println("Provider is not a Composite type : REVISIT TODO");                        
                    }                    
                }
                
                if (field.isMultiple())
                {
                    System.out.println("requires field has multiple instances: REVISIT TODO");                        
                }
                AMQPCompositeCodecUtils.generateDecoderForCompositeTypesMultipleProviders(outputStream, numberOfTabs+2, providers, canonicalFieldName);                
            }            
            else if (AMQPXMLReader.gPrimitiveTypes.contains(field.getType()))
            {
                AMQPCompositeCodecUtils.generateDecoderForPrimitiveTypes(outputStream, numberOfTabs+2, field.getType(), canonicalFieldName, field.isMultiple(), field.isRequired());
            }
            else if (AMQPXMLReader.gRestrictedTypes.contains(field.getType()))
            {
                AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(field.getType());
                String primTypeName = restrictedType.getType();
                AMQPCompositeCodecUtils.generateDecoderForPrimitiveTypes(outputStream, numberOfTabs+2, primTypeName, canonicalFieldName, field.isMultiple(), field.isRequired());  
            }
            else if (AMQPXMLReader.gDefinitionsCompositeTypes.contains(field.getType()))
            {
                AMQPCompositeCodecUtils.generateDecoderForCompositeTypes(outputStream, numberOfTabs+2, field.getType(), canonicalFieldName, field.isMultiple(), field.isRequired());                
            }   
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        }

        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));        
        outputStream.println(Utils.insertTabs(numberOfTabs, "return data;"));        
        outputStream.println(Utils.insertTabs(1, "}"));        
    }

    @Override
    protected void
    generateDecoderSignature()
    {
        outputStream.println(Utils.insertTabs(1, String.format("public static %s", type)));
        outputStream.println(Utils.insertTabs(1, String.format("    decode(CAMQPSyncDecoder decoder)")));
        outputStream.println(Utils.insertTabs(1, "{"));
        outputStream.println(Utils.insertTabs(2, "int formatCode;"));
        outputStream.println(Utils.insertTabs(2, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(2, "assert((formatCode == CAMQPFormatCodes.MAP8) || (formatCode == CAMQPFormatCodes.MAP32));"));
        outputStream.println();
        outputStream.println(Utils.insertTabs(2, "long mapSize = decoder.readCompoundSize(formatCode);"));
        outputStream.println(Utils.insertTabs(2, String.format("%s data = new %s();", type, type)));        
    }
    
    private void
    generateMapSize()
    {
        for (AMQPCompositeFieldType field : fields)
        {
            String hungarianFieldName = Utils.convertToCanonicalName(field.getName());
            String canonicalFieldName = Utils.lowerCapFirstLetter(hungarianFieldName);
            String requiredBooleanFieldName = String.format("isSet%s", hungarianFieldName);
            
            boolean isRequiredFlag = field.isRequired();
            boolean hasMultiples = field.isMultiple();
            
            int numberOfTabs = 2;
            
            if (field.getType().equalsIgnoreCase("Object"))
            {
                Collection<String> providers = field.getProviders();
                for (String provider : providers)
                {
                    if (!AMQPXMLReader.gDefinitionsCompositeTypes.contains(provider))
                    {
                        System.out.println("Provider is not a Composite type : REVISIT TODO");                        
                    }                    
                }
                
                if (hasMultiples)
                {
                    System.out.println("requires field has multiple instances: REVISIT TODO");                        
                }
                generateMapSizeForPrimitiveTypes(numberOfTabs, field.getType(), canonicalFieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples);
                
            }
            else if (AMQPXMLReader.gPrimitiveTypes.contains(field.getType()))
            {
                generateMapSizeForPrimitiveTypes(numberOfTabs, field.getType(), canonicalFieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples);
            }
            else if (AMQPXMLReader.gRestrictedTypes.contains(field.getType()))
            {
                AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(field.getType());
                String primTypeName = restrictedType.getType();
                generateMapSizeForPrimitiveTypes(numberOfTabs, primTypeName, canonicalFieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples);
            }
            else if (AMQPXMLReader.gDefinitionsCompositeTypes.contains(field.getType()))
            {
                generateMapSizeForCompositeTypes(numberOfTabs, field.getType(), canonicalFieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples);                
            }          
        }
    }
    
    private void
    generateMapSizeForPrimitiveTypes(int numberOfTabs, String primTypeName, String canonicalFieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples)
    {
        outputStream.println();
        if ((primTypeName.equalsIgnoreCase("binary")) || (primTypeName.equalsIgnoreCase("Object")))
        {
            generateMapSizeBinaryTypeEncoder(numberOfTabs, canonicalFieldName, requiredBooleanFieldName, isRequiredFlag);
        }
        else if (primTypeName.equalsIgnoreCase("map"))
        {
            generateMapSizeMapTypeEncoder(numberOfTabs, canonicalFieldName, requiredBooleanFieldName, isRequiredFlag);
        }
        else if ((primTypeName.equalsIgnoreCase("string")) || (primTypeName.equalsIgnoreCase("symbol")))
        {
            generateMapSizeStringOrSymbolEncoder(numberOfTabs, primTypeName, canonicalFieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples);
        }
        else
        {
            if (!isRequiredFlag)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s)", requiredBooleanFieldName)));
                generateIncrementMapSize(numberOfTabs);
            }            
        }
    }
    
    private void
    generateMapSizeForCompositeTypes(int numberOfTabs, String fieldType, String canonicalFieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples)    
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));                    
        }
        generateIncrementMapSize(numberOfTabs);        
    }
    
    private void
    generateMapSizeMapTypeEncoder(int numberOfTabs, String canonicalFieldName, String requiredBooleanFieldName, boolean isRequiredFlag)
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s.size() > 0))", canonicalFieldName, canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s.size() > 0) && (data.%s))", canonicalFieldName, canonicalFieldName, requiredBooleanFieldName)));            
        }
        generateIncrementMapSize(numberOfTabs);
    }
    
    private void
    generateMapSizeBinaryTypeEncoder(int numberOfTabs, String canonicalFieldName, String requiredBooleanFieldName, boolean isRequiredFlag)
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));            
        }
        generateIncrementMapSize(numberOfTabs);
    }
    
    private void
    generateMapSizeStringOrSymbolEncoder(int numberOfTabs, String primTypeName, String canonicalFieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples)
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));            
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));            
        }
        generateIncrementMapSize(numberOfTabs);
    }
    
    private void
    generateIncrementMapSize(int numberOfTabs)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "mapSize++;"));        
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }    
}
