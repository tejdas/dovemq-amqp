package net.dovemq.transport.protocol.data;
import net.dovemq.transport.protocol.Width;

public enum CAMQPTypes
{
    VBIN8 (CAMQPFormatCodes.VBIN8, "VBIN8", Width.FIXED_ONE),
    VBIN32 (CAMQPFormatCodes.VBIN32, "VBIN32", Width.FIXED_FOUR),
    SYM8 (CAMQPFormatCodes.SYM8, "SYM8", Width.FIXED_ONE),
    SYM32 (CAMQPFormatCodes.SYM32, "SYM32", Width.FIXED_FOUR),
    CHAR (CAMQPFormatCodes.CHAR, "CHAR", Width.FIXED_FOUR),
    INT (CAMQPFormatCodes.INT, "INT", Width.FIXED_FOUR),
    SMALLINT (CAMQPFormatCodes.SMALLINT, "SMALLINT", Width.FIXED_ONE),
    LIST8 (CAMQPFormatCodes.LIST8, "LIST8", Width.FIXED_ONE),
    LIST32 (CAMQPFormatCodes.LIST32, "LIST32", Width.FIXED_FOUR),
    DECIMAL64 (CAMQPFormatCodes.DECIMAL64, "DECIMAL64", Width.FIXED_EIGHT),
    USHORT (CAMQPFormatCodes.USHORT, "USHORT", Width.FIXED_TWO),
    LONG (CAMQPFormatCodes.LONG, "LONG", Width.FIXED_EIGHT),
    SMALLLONG (CAMQPFormatCodes.SMALLLONG, "SMALLLONG", Width.FIXED_ONE),
    DOUBLE (CAMQPFormatCodes.DOUBLE, "DOUBLE", Width.FIXED_EIGHT),
    FLOAT (CAMQPFormatCodes.FLOAT, "FLOAT", Width.FIXED_FOUR),
    TIMESTAMP (CAMQPFormatCodes.TIMESTAMP, "TIMESTAMP", Width.FIXED_EIGHT),
    SHORT (CAMQPFormatCodes.SHORT, "SHORT", Width.FIXED_TWO),
    BYTE (CAMQPFormatCodes.BYTE, "BYTE", Width.FIXED_ONE),
    STR8_UTF8 (CAMQPFormatCodes.STR8_UTF8, "STR8_UTF8", Width.FIXED_ONE),
    STR32_UTF8 (CAMQPFormatCodes.STR32_UTF8, "STR32_UTF8", Width.FIXED_FOUR),
    MAP8 (CAMQPFormatCodes.MAP8, "MAP8", Width.FIXED_ONE),
    MAP32 (CAMQPFormatCodes.MAP32, "MAP32", Width.FIXED_FOUR),
    BOOLEAN (CAMQPFormatCodes.BOOLEAN, "BOOLEAN", Width.FIXED_ONE),
    TRUE (CAMQPFormatCodes.TRUE, "TRUE", Width.FIXED_ZERO),
    FALSE (CAMQPFormatCodes.FALSE, "FALSE", Width.FIXED_ZERO),
    UUID (CAMQPFormatCodes.UUID, "UUID", Width.FIXED_SIXTEEN),
    UBYTE (CAMQPFormatCodes.UBYTE, "UBYTE", Width.FIXED_ONE),
    ULONG (CAMQPFormatCodes.ULONG, "ULONG", Width.FIXED_EIGHT),
    SMALLULONG (CAMQPFormatCodes.SMALLULONG, "SMALLULONG", Width.FIXED_ONE),
    ULONG0 (CAMQPFormatCodes.ULONG0, "ULONG0", Width.FIXED_ZERO),
    DECIMAL128 (CAMQPFormatCodes.DECIMAL128, "DECIMAL128", Width.FIXED_SIXTEEN),
    NULL (CAMQPFormatCodes.NULL, "NULL", Width.FIXED_ZERO),
    UINT (CAMQPFormatCodes.UINT, "UINT", Width.FIXED_FOUR),
    SMALLUINT (CAMQPFormatCodes.SMALLUINT, "SMALLUINT", Width.FIXED_ONE),
    UINT0 (CAMQPFormatCodes.UINT0, "UINT0", Width.FIXED_ZERO),
    ARRAY8 (CAMQPFormatCodes.ARRAY8, "ARRAY8", Width.FIXED_ONE),
    ARRAY32 (CAMQPFormatCodes.ARRAY32, "ARRAY32", Width.FIXED_FOUR),
    DECIMAL32 (CAMQPFormatCodes.DECIMAL32, "DECIMAL32", Width.FIXED_FOUR),
    ;
    private final int formatCode;
    private final String typeName;
    private final Width width;
    CAMQPTypes(int formatCode, String typeName, Width width)
    {
        this.formatCode = formatCode;
        this.typeName = typeName;
        this.width = width;
    }
    protected int formatCode()
    {
        return formatCode;
    }
    protected String typeName()
    {
        return typeName;
    }
    protected Width width()
    {
        return width;
    }
}
