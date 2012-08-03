package net.dovemq.transport.protocol;

public class CAMQPCompundHeader
{
    public final int elementFormatCode;
    public final long elementCount;
    public CAMQPCompundHeader(int elementFormatCode, long elementCount)
    {
        super();
        this.elementFormatCode = elementFormatCode;
        this.elementCount = elementCount;
    }
}
