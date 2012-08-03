package net.dovemq.transport.protocol.data;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionHeader
{
    public static final String descriptor = "amqp:header:list";

    private boolean isSetDurable = false;
    public void setRequiredDurable(boolean val)
    {
        isSetDurable = val;
    }
    public boolean isSetDurable()
    {
        return isSetDurable;
    }
    private Boolean durable = false;
    public void setDurable(Boolean val)
    {
        isSetDurable = true;
        durable = val;
    }

    public Boolean getDurable()
    {
        return durable;
    }

    private boolean isSetPriority = false;
    public void setRequiredPriority(boolean val)
    {
        isSetPriority = val;
    }
    public boolean isSetPriority()
    {
        return isSetPriority;
    }
    private int priority = 0;
    public void setPriority(int val)
    {
        isSetPriority = true;
        priority = val;
    }

    public int getPriority()
    {
        return priority;
    }

    private boolean isSetTtl = false;
    public void setRequiredTtl(boolean val)
    {
        isSetTtl = val;
    }
    public boolean isSetTtl()
    {
        return isSetTtl;
    }
    private Long ttl = 0L;
    public void setTtl(Long val)
    {
        isSetTtl = true;
        ttl = val;
    }

    public Long getTtl()
    {
        return ttl;
    }

    private boolean isSetFirstAcquirer = false;
    public void setRequiredFirstAcquirer(boolean val)
    {
        isSetFirstAcquirer = val;
    }
    public boolean isSetFirstAcquirer()
    {
        return isSetFirstAcquirer;
    }
    private Boolean firstAcquirer = false;
    public void setFirstAcquirer(Boolean val)
    {
        isSetFirstAcquirer = true;
        firstAcquirer = val;
    }

    public Boolean getFirstAcquirer()
    {
        return firstAcquirer;
    }

    private boolean isSetDeliveryCount = false;
    public void setRequiredDeliveryCount(boolean val)
    {
        isSetDeliveryCount = val;
    }
    public boolean isSetDeliveryCount()
    {
        return isSetDeliveryCount;
    }
    private Long deliveryCount = 0L;
    public void setDeliveryCount(Long val)
    {
        isSetDeliveryCount = true;
        deliveryCount = val;
    }

    public Long getDeliveryCount()
    {
        return deliveryCount;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionHeader data)
    {
        long listSize = 5;
        encoder.writeListDescriptor(descriptor, listSize);

        if (data.isSetDurable)
        {
            encoder.writeBoolean(data.durable);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetPriority)
        {
            encoder.writeUByte(data.priority);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetTtl)
        {
            encoder.writeUInt(data.ttl);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetFirstAcquirer)
        {
            encoder.writeBoolean(data.firstAcquirer);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetDeliveryCount)
        {
            encoder.writeUInt(data.deliveryCount);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionHeader decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 5);
        CAMQPDefinitionHeader data = new CAMQPDefinitionHeader();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.durable = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetDurable = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.priority = decoder.readUByte();
            data.isSetPriority = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.ttl = decoder.readUInt();
            data.isSetTtl = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.firstAcquirer = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetFirstAcquirer = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.deliveryCount = decoder.readUInt();
            data.isSetDeliveryCount = true;
        }
        return data;
    }
}
