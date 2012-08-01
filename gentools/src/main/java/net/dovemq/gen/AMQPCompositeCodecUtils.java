package net.dovemq.gen;

import java.io.PrintWriter;
import java.util.Collection;

public class AMQPCompositeCodecUtils
{
    protected static void
    generateEncoderForPrimitiveTypes(PrintWriter outputStream, int numberOfTabs, String primTypeName, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples, boolean isCompositeTypeList)
    {
        outputStream.println();
        if (primTypeName.equalsIgnoreCase("binary"))
        {
            generateBinaryTypeEncoder(outputStream, numberOfTabs, canonicalFieldName, fieldName, requiredBooleanFieldName, isRequiredFlag, isCompositeTypeList);
        }
        else if (primTypeName.equalsIgnoreCase("map"))
        {
            generateMapCompositeTypeEncoder(outputStream, numberOfTabs, canonicalFieldName, fieldName, requiredBooleanFieldName, isRequiredFlag, isCompositeTypeList);
        }
        else if ((primTypeName.equalsIgnoreCase("string")) || (primTypeName.equalsIgnoreCase("symbol")))
        {
            generateStringOrSymbolEncoder(outputStream, numberOfTabs, primTypeName, canonicalFieldName, fieldName, requiredBooleanFieldName, isRequiredFlag, hasMultiples, isCompositeTypeList);
        }
        else
        {
            String codecMethodName = AMQPPrimitiveTypeMappings.getCodecMethodName(primTypeName);
            if (isRequiredFlag)
            {
                if (!isCompositeTypeList)
                {
                    outputStream.println(Utils.insertTabs(numberOfTabs, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
                }
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("encoder.write%s(data.%s);", codecMethodName, canonicalFieldName)));
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s)", requiredBooleanFieldName)));
                outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
                if (!isCompositeTypeList)
                {
                    outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
                }
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.write%s(data.%s);", codecMethodName, canonicalFieldName)));
                outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
                if (isCompositeTypeList)
                {
                    outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
                    generateWriteNull(outputStream, numberOfTabs);
                }
            }
        }
    }
    
    protected static void
    generateEncoderForCompositeTypes(PrintWriter outputStream, int numberOfTabs, String fieldType, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples, boolean isCompositeTypeList)    
    {
        AMQPCompositeType compoundType = (AMQPCompositeType) AMQPXMLReader.gDefinitionsCompositeTypes.getType(fieldType);
        String typeName = compoundType.getType();
        
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));                    
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (hasMultiples)
        {
            generateCompositeRepeatingTypeEncoder(outputStream, numberOfTabs, typeName, canonicalFieldName, null);
            if (isCompositeTypeList)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, "else"));
                generateWriteNull(outputStream, numberOfTabs+1);
            }
        }
        else
        {
            if (!isCompositeTypeList)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("%s.encode(encoder, data.%s);", typeName, canonicalFieldName)));
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
            generateWriteNull(outputStream, numberOfTabs);
        }
    }

    protected static void
    generateEncoderForCompositeTypesMultipleProviders(PrintWriter outputStream, int numberOfTabs, Collection<String> providers, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean isCompositeTypeList)    
    {
        outputStream.println();        
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));                    
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (!isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
        }        
        boolean firstLoop = true;
        for (String provider : providers)
        {
            AMQPCompositeType compoundType = (AMQPCompositeType) AMQPXMLReader.gDefinitionsCompositeTypes.getType(provider);
            if (compoundType == null)
            {
                System.out.println("Provider not available for compound type: " + provider);
                continue;
            }
            String typeName = compoundType.getType();
            if (firstLoop)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (data.%s instanceof %s)", canonicalFieldName, typeName)));
                firstLoop = false;
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("else if (data.%s instanceof %s)", canonicalFieldName, typeName)));                
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));            
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("%s.encode(encoder, (%s) data.%s);", typeName, typeName, canonicalFieldName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));            
        }

        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
            generateWriteNull(outputStream, numberOfTabs);
        }
    }

    protected static void
    generateEncoderForRestrictedTypesMultipleProviders(PrintWriter outputStream, int numberOfTabs, Collection<String> providers, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean isCompositeTypeList)    
    {
        outputStream.println();        
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));                    
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (!isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
        }        
        boolean firstLoop = true;
        for (String provider : providers)
        {
            AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(provider);
            if (restrictedType == null)
            {
                System.out.println("Provider not available for restricted type: " + provider);
                continue;
            }
            String typeName = AMQPPrimitiveTypeMappings.getJavaType(restrictedType.getType()); 
            String codecName = AMQPPrimitiveTypeMappings.getCodecMethodName(restrictedType.getType());
            if (firstLoop)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (data.%s instanceof %s)", canonicalFieldName, typeName)));
                firstLoop = false;
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("else if (data.%s instanceof %s)", canonicalFieldName, typeName)));                
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));      
            if (restrictedType.getType().equalsIgnoreCase("binary"))
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("byte[] binData = (byte[]) data.%s;", canonicalFieldName)));  
                outputStream.println(Utils.insertTabs(numberOfTabs+2, "encoder.writeBinary(binData, binData.length, false);"));     
            }
            else
                outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.write%s((%s) data.%s);", codecName, typeName, canonicalFieldName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));            
        }

        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
            generateWriteNull(outputStream, numberOfTabs);
        }
    }
    
    protected static void
    generateDecoderForCompositeTypes(PrintWriter outputStream, int numberOfTabs, String fieldType, String canonicalFieldName, boolean isMultiple, boolean isRequired)
    {
        AMQPCompositeType compoundType = (AMQPCompositeType) AMQPXMLReader.gDefinitionsCompositeTypes.getType(fieldType);
        String typeName = compoundType.getType();
 
        if (isMultiple)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();"));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)"));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
            outputStream.println(Utils.insertTabs(numberOfTabs+2, "assert(decoder.isNextDescribedConstructor());"));
            outputStream.println(Utils.insertTabs(numberOfTabs+2, "String controlName = decoder.readSymbol();"));
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("assert(controlName.equalsIgnoreCase(%s.descriptor));", typeName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("%s singleVal = %s.decode(decoder);", typeName, typeName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("data.%s.add(singleVal);", canonicalFieldName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
            outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
            
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "if (decoder.isNextDescribedConstructor())"));        
            outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "String controlName = decoder.readSymbol();"));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("assert(controlName.equalsIgnoreCase(%s.descriptor));", typeName)));
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.%s = %s.decode(decoder);", canonicalFieldName, typeName)));
            
            if (!isRequired)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.isSet%s = true;", Utils.upperCapFirstLetter(canonicalFieldName))));                    
            }
            
            outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
            generateReadNullFormatCode(outputStream, numberOfTabs);
        }
    }
    
    protected static void
    generateDecoderForCompositeTypesMultipleProviders(PrintWriter outputStream, int numberOfTabs, Collection<String> providers, String canonicalFieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "if (decoder.isNextDescribedConstructor())"));        
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));        
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "String controlName = decoder.readSymbol();"));
        boolean firstLoop = true;
        for (String provider : providers)
        {
            AMQPCompositeType compoundType = (AMQPCompositeType) AMQPXMLReader.gDefinitionsCompositeTypes.getType(provider);
            if (compoundType == null)
            {
                System.out.println("Provider not available for compound type: " + provider);
                continue;
            }
            String typeName = compoundType.getType();
            if (firstLoop)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (controlName.equalsIgnoreCase(%s.descriptor))", typeName)));
                firstLoop = false;
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("else if (controlName.equalsIgnoreCase(%s.descriptor))", typeName)));                
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));            
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("data.%s = %s.decode(decoder);", canonicalFieldName, typeName)));            
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
        generateReadNullFormatCode(outputStream, numberOfTabs);
    }    
    
    protected static void
    generateDecoderForRestrictedTypesMultipleProviders(PrintWriter outputStream, int numberOfTabs, Collection<String> providers, String canonicalFieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "if (formatCode != CAMQPFormatCodes.NULL)"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));        
        boolean firstLoop = true;
        for (String provider : providers)
        {
            AMQPRestrictedType restrictedType = (AMQPRestrictedType) AMQPXMLReader.gRestrictedTypes.getType(provider);
            if (restrictedType == null)
            {
                System.out.println("Provider not available for restricted type: " + provider);
                continue;
            }

            String primTypeName = restrictedType.getType();
            String formatCodeName = AMQPPrimitiveTypeMappings.getFormatCode(primTypeName);

            if (firstLoop)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (formatCode == %s)", formatCodeName)));
                firstLoop = false;
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("else if (formatCode == %s)", formatCodeName)));                
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "{")); 
            //AMQPCompositeCodecUtils.generateDecoderForPrimitiveTypes(outputStream, numberOfTabs+2, primTypeName, canonicalFieldName, false, true);  
            AMQPCompositeCodecUtils.generateDecoderForPrimitiveProviderTypes(outputStream, numberOfTabs+2, primTypeName, canonicalFieldName);
            outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }
    
    protected static void
    generateDecoderForPrimitiveTypes(PrintWriter outputStream, int numberOfTabs, String primTypeName, String canonicalFieldName, boolean isMultiple, boolean isRequired)
    {
        if (!isMultiple)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "formatCode = decoder.readFormatCode();"));
            outputStream.println(Utils.insertTabs(numberOfTabs, "if (formatCode != CAMQPFormatCodes.NULL)"));
            outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        }
        
        if (primTypeName.equalsIgnoreCase("binary"))
        {
            boolean isProvider = false;
            generateBinaryTypeDecoder(outputStream, numberOfTabs+1, canonicalFieldName, isProvider);
        }
        else if (primTypeName.equalsIgnoreCase("map"))
        {
            generateMapTypeDecoder(outputStream, numberOfTabs+1, canonicalFieldName);
        }
        else if (primTypeName.equalsIgnoreCase("string"))
        {
            if (isMultiple)
            {
                generateDecoderForRepeatingElements(outputStream, numberOfTabs, canonicalFieldName);
            }
            else
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.%s = decoder.readString(formatCode);", canonicalFieldName)));
            }
        }        
        else if (primTypeName.equalsIgnoreCase("boolean"))
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.%s = (formatCode == CAMQPFormatCodes.TRUE);", canonicalFieldName)));            
        }
        else
        {
            String codecMethodName = AMQPPrimitiveTypeMappings.getCodecMethodName(primTypeName);
            
            if (isMultiple)
            {
                if (primTypeName.equals("symbol"))
                {
                    generateDecoderForRepeatingElements(outputStream, numberOfTabs, canonicalFieldName);
                }
                else
                {
                    // REVISIT TODO
                    System.out.println("COMPOSITE: isMultiple REVISIT THIS: " + canonicalFieldName + "  " + primTypeName + "   " + codecMethodName);
                }           
            }
            else
            {
                if (primTypeName.equals("symbol"))
                {
                    outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.%s = decoder.readString(formatCode);", canonicalFieldName)));
                }
                else
                {
                    outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.%s = decoder.read%s();", canonicalFieldName, codecMethodName)));                    
                }                
            }
        }
        
        if (!isMultiple)
        {
            if (!isRequired)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("data.isSet%s = true;", Utils.upperCapFirstLetter(canonicalFieldName))));                    
            }
            outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        }
    }

    private static void
    generateDecoderForPrimitiveProviderTypes(PrintWriter outputStream, int numberOfTabs, String primTypeName, String canonicalFieldName)
    {
        if (primTypeName.equalsIgnoreCase("binary"))
        {
            boolean isProvider = true;
            generateBinaryTypeDecoder(outputStream, numberOfTabs, canonicalFieldName, isProvider);
        }
        else if (primTypeName.equalsIgnoreCase("map"))
            generateMapTypeDecoder(outputStream, numberOfTabs, canonicalFieldName);
        else if (primTypeName.equalsIgnoreCase("string"))
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("data.%s = decoder.readString(formatCode);", canonicalFieldName)));        
        else if (primTypeName.equalsIgnoreCase("boolean"))
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("data.%s = (formatCode == CAMQPFormatCodes.TRUE);", canonicalFieldName)));            
        else
        {
            String codecMethodName = AMQPPrimitiveTypeMappings.getCodecMethodName(primTypeName);
            if (primTypeName.equals("symbol"))
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("data.%s = decoder.readString(formatCode);", canonicalFieldName)));
            else
                outputStream.println(Utils.insertTabs(numberOfTabs, String.format("data.%s = decoder.read%s();", canonicalFieldName, codecMethodName)));
        }
    }
    
    private static void
    generateMapCompositeTypeEncoder(PrintWriter outputStream, int numberOfTabs, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean isCompositeTypeList)
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s.size() > 0))", canonicalFieldName, canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s.size() > 0) && (data.%s))", canonicalFieldName, canonicalFieldName, requiredBooleanFieldName)));            
        }
        generateMapTypeEncoder(outputStream, numberOfTabs, canonicalFieldName, fieldName);
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));        
            generateWriteNull(outputStream, numberOfTabs);
        }
    }
    
    private static void
    generateBinaryTypeEncoder(PrintWriter outputStream, int numberOfTabs, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag,  boolean isCompositeTypeList)
    {
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (!isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
        }                
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeBinary(data.%s, data.%s.length, false);", canonicalFieldName, canonicalFieldName)));      
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));
            generateWriteNull(outputStream, numberOfTabs);
        }
        
    }    
    
    private static void
    generateStringOrSymbolEncoder(PrintWriter outputStream, int numberOfTabs, String primTypeName, String canonicalFieldName, String fieldName, String requiredBooleanFieldName, boolean isRequiredFlag, boolean hasMultiples,  boolean isCompositeTypeList)
    {
        String codecMethodName = AMQPPrimitiveTypeMappings.getCodecMethodName(primTypeName);
        if (isRequiredFlag)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if (data.%s != null)", canonicalFieldName)));            
        }
        else
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("if ((data.%s != null) && (data.%s))", canonicalFieldName, requiredBooleanFieldName)));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (hasMultiples)
        {
            generateStringOrSymbolRepeatingTypeEncoder(outputStream, numberOfTabs, primTypeName, canonicalFieldName, fieldName, codecMethodName);
            if (isCompositeTypeList)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, "else"));
                generateWriteNull(outputStream, numberOfTabs+1);
            }
        }
        else
        {
            if (!isCompositeTypeList)
            {
                outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));
            }
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.write%s(data.%s);", codecMethodName, canonicalFieldName)));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
        if (isCompositeTypeList)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs, "else"));            
            generateWriteNull(outputStream, numberOfTabs);
        }
    }

    private static void
    generateWriteNull(PrintWriter outputStream, int numberOfTabs)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "encoder.writeNull();"));            
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }
    
    private static void
    generateStringOrSymbolRepeatingTypeEncoder(PrintWriter outputStream, int numberOfTabs, String primTypeName, String canonicalFieldName, String fieldName, String codecMethodName)
    {
        String repeatingCodecMethodName = AMQPPrimitiveTypeMappings.getRepeatingCodecMethodName(primTypeName);
        String typeName = AMQPPrimitiveTypeMappings.getJavaType(primTypeName);
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("long %sSize = data.%s.size();", canonicalFieldName, canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (%sSize > 0)", canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        if (fieldName != null)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.writeSymbol(\"%s\");", fieldName)));            
        }
        String formatCodeStr = (primTypeName.contains("symbol"))? "CAMQPFormatCodes.SYM8" : "CAMQPFormatCodes.STR8_UTF8";
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.writeArrayHeaderForMultiple(%sSize, %s);", canonicalFieldName, formatCodeStr)));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("for (%s val : data.%s)", typeName, canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "{"));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+3, String.format("encoder.write%s(val);", repeatingCodecMethodName)));            
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.fillCompoundSize(%sSize);", canonicalFieldName)));// TODO tejdas
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
    }
    
    private static void
    generateCompositeRepeatingTypeEncoder(PrintWriter outputStream, int numberOfTabs, String typeName, String canonicalFieldName, String fieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("long %sSize = data.%s.size();", canonicalFieldName, canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("if (%sSize > 0)", canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        if (fieldName != null)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.writeSymbol(\"%s\");", fieldName)));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("int formatCode = (%sSize <= 255) ? CAMQPFormatCodes.LIST8 : CAMQPFormatCodes.LIST32;", canonicalFieldName))); 
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.writeArrayHeaderForMultiple(%sSize, formatCode);", canonicalFieldName)));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("for (%s val : data.%s)", typeName, canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "{"));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+3, String.format("%s.encode(encoder, val);", typeName)));          
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("encoder.fillCompoundSize(%sSize);", canonicalFieldName)));// TODO tejdas
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
    }
    
    private static void
    generateMapTypeEncoder(PrintWriter outputStream, int numberOfTabs, String canonicalFieldName, String fieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        if (fieldName != null)
        {
            outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("encoder.writeSymbol(\"%s\");", fieldName)));            
        }
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("int size = data.%s.size();", canonicalFieldName)));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "encoder.writeMapHeader(size);"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, String.format("Set<Entry<String, String>> entries = data.%s.entrySet();", canonicalFieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "for (Entry<String, String> entry : entries)"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "encoder.writeSymbol(entry.getKey());"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "encoder.writeUTF8String(entry.getValue());"));                    
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "encoder.fillCompoundSize(size);")); // TODO tejdas        
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }
    
    private static void
    generateReadNullFormatCode(PrintWriter outputStream, int numberOfTabs)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "assert (formatCode == CAMQPFormatCodes.NULL);"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }
    
    private static void
    generateDecoderForRepeatingElements(PrintWriter outputStream, int numberOfTabs, String fieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "CAMQPCompundHeader compoundHeader = decoder.readMultipleElementCount();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "for (int innerIndex = 0; innerIndex < compoundHeader.elementCount; innerIndex++)"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("data.%s.add(decoder.readString(compoundHeader.elementFormatCode));", fieldName)));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "}")); 
    }
  
    private static void
    generateBinaryTypeDecoder(PrintWriter outputStream, int numberOfTabs, String canonicalFieldName, boolean isProvider)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "int size = (int) decoder.readBinaryDataSize(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);"));
        outputStream.println(Utils.insertTabs(numberOfTabs, String.format("data.%s = new byte[size];", canonicalFieldName)));
        if (isProvider)
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("channelBuf.readBytes((byte[]) data.%s);", canonicalFieldName)));
        else
            outputStream.println(Utils.insertTabs(numberOfTabs, String.format("channelBuf.readBytes(data.%s);", canonicalFieldName)));
    }
    
    private static void
    generateMapTypeDecoder(PrintWriter outputStream, int numberOfTabs, String canonicalFieldName)
    {
        outputStream.println(Utils.insertTabs(numberOfTabs, "assert((formatCode == CAMQPFormatCodes.MAP8) || (formatCode == CAMQPFormatCodes.MAP32));"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "long innerMapSize = decoder.readMapCount(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "for (long innerIndex = 0; innerIndex < innerMapSize; innerIndex++)")); 
        outputStream.println(Utils.insertTabs(numberOfTabs, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "String innerKey = null;"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "if (formatCode == CAMQPFormatCodes.SYM8)"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "innerKey = decoder.readString(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "String innerVal = null;"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "formatCode = decoder.readFormatCode();"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "if (formatCode != CAMQPFormatCodes.NULL)"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, "innerVal = decoder.readString(formatCode);"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "if ((innerKey != null) && (innerVal != null))"));
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "{"));
        outputStream.println(Utils.insertTabs(numberOfTabs+2, String.format("data.%s.put(innerKey, innerVal);", canonicalFieldName)));                       
        outputStream.println(Utils.insertTabs(numberOfTabs+1, "}"));
        outputStream.println(Utils.insertTabs(numberOfTabs, "}"));
    }    
}
