package net.dovemq.gen;

import java.util.HashMap;
import java.util.Map;

class AMQPPrimitiveTypeMappings
{
    private static final Map<String, String> AMQP_TYPE_TO_JAVA_TYPE_MAP = new HashMap<String, String>();
    private static final Map<String, String> AMQP_TYPE_TO_CODEC_METHOD_MAP = new HashMap<String, String>();
    private static final Map<String, String> AMQP_REPEATING_TYPE_TO_CODEC_METHOD_MAP = new HashMap<String, String>();
    private static final Map<String, String> AMQP_TYPE_TO_DEFAULT_VALUE_MAP = new HashMap<String, String>();   
    private static final Map<String, String> AMQP_TYPE_TO_FORMAT_CODE_MAP = new HashMap<String, String>();
    
    static final void
    initialize()
    {
        AMQP_TYPE_TO_FORMAT_CODE_MAP.put("ulong", "CAMQPFormatCodes.LONG");
        AMQP_TYPE_TO_FORMAT_CODE_MAP.put("uuid", "CAMQPFormatCodes.UUID");
        AMQP_TYPE_TO_FORMAT_CODE_MAP.put("binary", "CAMQPFormatCodes.VBIN8");
        AMQP_TYPE_TO_FORMAT_CODE_MAP.put("string", "CAMQPFormatCodes.STR8_UTF8");
        
        AMQP_REPEATING_TYPE_TO_CODEC_METHOD_MAP.put("string", "UTF8StringArrayElement");
        AMQP_REPEATING_TYPE_TO_CODEC_METHOD_MAP.put("symbol", "SymbolArrayElement");
        
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("boolean", "Boolean");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("ubyte", "int");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("ushort", "Integer");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("uint", "Long");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("ulong", "java.math.BigInteger");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("byte", "Byte");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("short", "Short");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("int", "Integer");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("long", "Long");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("float", "Float");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("double", "Double");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("char", "Character");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("timestamp", "Date");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("uuid", "java.util.UUID");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("binary", "byte[]");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("string", "String");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("list", "List");  
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("map", "Map<String, String>");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("symbol", "String");
        AMQP_TYPE_TO_JAVA_TYPE_MAP.put("Object", "Object");        
        
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("boolean", "Boolean");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("ubyte", "UByte");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("ushort", "UShort");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("uint", "UInt");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("ulong", "ULong");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("byte", "Byte");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("short", "Short");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("int", "Int");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("long", "Long");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("float", "Float");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("double", "Double");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("char", "Character");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("timestamp", "TimeStamp");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("uuid", "UUID");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("string", "UTF8String");
        AMQP_TYPE_TO_CODEC_METHOD_MAP.put("symbol", "Symbol");      
        
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("boolean", "false");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("ubyte", "0");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("ushort", "0");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("uint", "0L");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("ulong", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("byte", "0");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("short", "0");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("int", "0");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("long", "0L");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("float", "0F");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("double", "0D");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("char", "");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("timestamp", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("uuid", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("binary", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("string", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("list", "null");  
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("map", "null");
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("symbol", "null");        
        AMQP_TYPE_TO_DEFAULT_VALUE_MAP.put("Object", "null");        
    };

    static String
    getJavaType(String amqpType)
    {
        return AMQP_TYPE_TO_JAVA_TYPE_MAP.get(amqpType);
    }

    static String
    getCodecMethodName(String amqpType)
    {
        return AMQP_TYPE_TO_CODEC_METHOD_MAP.get(amqpType);        
    }    
    
    static String
    getRepeatingCodecMethodName(String amqpType)
    {
        return AMQP_REPEATING_TYPE_TO_CODEC_METHOD_MAP.get(amqpType);    
    } 
    
    static String
    getFormatCode(String amqpType)
    {
        return AMQP_TYPE_TO_FORMAT_CODE_MAP.get(amqpType);    
    }
    
    static String
    getDefaultValue(String amqpType)
    {
        return AMQP_TYPE_TO_DEFAULT_VALUE_MAP.get(amqpType);        
    }    
}
