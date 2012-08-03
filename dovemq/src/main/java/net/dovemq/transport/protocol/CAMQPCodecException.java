package net.dovemq.transport.protocol;

import net.dovemq.transport.protocol.data.CAMQPTypes;

public class CAMQPCodecException extends RuntimeException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final CAMQPTypes type;
    private final int formatCode;
    
    public CAMQPCodecException(CAMQPTypes type, int formatCode)
    {
        super();
        this.type = type;
        this.formatCode = formatCode;
    }
    public CAMQPTypes getType()
    {
        return type;
    }
    public int getFormatCode()
    {
        return formatCode;
    }
}
