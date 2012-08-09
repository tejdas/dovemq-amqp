/**
 * This file was auto-generated by dovemq gentools.
 * Do not modify.
 */
package net.dovemq.transport.protocol.data;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPControlTransfer
{
    public static final String descriptor = "amqp:transfer:list";

    private Long handle = 0L;
    public void setHandle(Long val)
    {
        handle = val;
    }

    public Long getHandle()
    {
        return handle;
    }

    private Long deliveryId = 0L;
    public void setDeliveryId(Long val)
    {
        deliveryId = val;
    }

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    private boolean isSetDeliveryTag = false;
    public void setRequiredDeliveryTag(boolean val)
    {
        isSetDeliveryTag = val;
    }
    public boolean isSetDeliveryTag()
    {
        return isSetDeliveryTag;
    }
    private byte[] deliveryTag = null;
    public void setDeliveryTag(byte[] val)
    {
        isSetDeliveryTag = true;
        deliveryTag = val;
    }

    public byte[] getDeliveryTag()
    {
        return deliveryTag;
    }

    private boolean isSetMessageFormat = false;
    public void setRequiredMessageFormat(boolean val)
    {
        isSetMessageFormat = val;
    }
    public boolean isSetMessageFormat()
    {
        return isSetMessageFormat;
    }
    private Long messageFormat = 0L;
    public void setMessageFormat(Long val)
    {
        isSetMessageFormat = true;
        messageFormat = val;
    }

    public Long getMessageFormat()
    {
        return messageFormat;
    }

    private boolean isSetSettled = false;
    public void setRequiredSettled(boolean val)
    {
        isSetSettled = val;
    }
    public boolean isSetSettled()
    {
        return isSetSettled;
    }
    private Boolean settled = false;
    public void setSettled(Boolean val)
    {
        isSetSettled = true;
        settled = val;
    }

    public Boolean getSettled()
    {
        return settled;
    }

    private boolean isSetRcvSettleMode = false;
    public void setRequiredRcvSettleMode(boolean val)
    {
        isSetRcvSettleMode = val;
    }
    public boolean isSetRcvSettleMode()
    {
        return isSetRcvSettleMode;
    }
    private int rcvSettleMode = 0;
    public void setRcvSettleMode(int val)
    {
        isSetRcvSettleMode = true;
        rcvSettleMode = val;
    }

    public int getRcvSettleMode()
    {
        return rcvSettleMode;
    }

    private boolean isSetState = false;
    public void setRequiredState(boolean val)
    {
        isSetState = val;
    }
    public boolean isSetState()
    {
        return isSetState;
    }
    private Object state = null;
    public void setState(Object val)
    {
        isSetState = true;
        state = val;
    }

    public Object getState()
    {
        return state;
    }

    private boolean isSetResume = false;
    public void setRequiredResume(boolean val)
    {
        isSetResume = val;
    }
    public boolean isSetResume()
    {
        return isSetResume;
    }
    private Boolean resume = false;
    public void setResume(Boolean val)
    {
        isSetResume = true;
        resume = val;
    }

    public Boolean getResume()
    {
        return resume;
    }

    private boolean isSetMore = false;
    public void setRequiredMore(boolean val)
    {
        isSetMore = val;
    }
    public boolean isSetMore()
    {
        return isSetMore;
    }
    private Boolean more = false;
    public void setMore(Boolean val)
    {
        isSetMore = true;
        more = val;
    }

    public Boolean getMore()
    {
        return more;
    }

    private boolean isSetAborted = false;
    public void setRequiredAborted(boolean val)
    {
        isSetAborted = val;
    }
    public boolean isSetAborted()
    {
        return isSetAborted;
    }
    private Boolean aborted = false;
    public void setAborted(Boolean val)
    {
        isSetAborted = true;
        aborted = val;
    }

    public Boolean getAborted()
    {
        return aborted;
    }

    private boolean isSetBatchable = false;
    public void setRequiredBatchable(boolean val)
    {
        isSetBatchable = val;
    }
    public boolean isSetBatchable()
    {
        return isSetBatchable;
    }
    private Boolean batchable = false;
    public void setBatchable(Boolean val)
    {
        isSetBatchable = true;
        batchable = val;
    }

    public Boolean getBatchable()
    {
        return batchable;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPControlTransfer data)
    {
        long listSize = 11;
        encoder.writeListDescriptor(descriptor, listSize);

        encoder.writeUInt(data.handle);

        encoder.writeUInt(data.deliveryId);

        if ((data.deliveryTag != null) && (data.isSetDeliveryTag))
        {
            encoder.writeBinary(data.deliveryTag, data.deliveryTag.length, false);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetMessageFormat)
        {
            encoder.writeUInt(data.messageFormat);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetSettled)
        {
            encoder.writeBoolean(data.settled);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetRcvSettleMode)
        {
            encoder.writeUByte(data.rcvSettleMode);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.state != null) && (data.isSetState))
        {
            if (data.state instanceof CAMQPDefinitionDeliveryState)
            {
                CAMQPDefinitionDeliveryState.encode(encoder, (CAMQPDefinitionDeliveryState) data.state);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetResume)
        {
            encoder.writeBoolean(data.resume);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetMore)
        {
            encoder.writeBoolean(data.more);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetAborted)
        {
            encoder.writeBoolean(data.aborted);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetBatchable)
        {
            encoder.writeBoolean(data.batchable);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPControlTransfer decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 11);
        CAMQPControlTransfer data = new CAMQPControlTransfer();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.handle = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.deliveryId = decoder.readUInt();
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.deliveryTag = new byte[size];
            channelBuf.readBytes(data.deliveryTag);
            data.isSetDeliveryTag = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.messageFormat = decoder.readUInt();
            data.isSetMessageFormat = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.settled = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetSettled = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.rcvSettleMode = decoder.readUByte();
            data.isSetRcvSettleMode = true;
        }

        if (decoder.isNextDescribedConstructor())
        {
            String controlName = decoder.readSymbol();
            if (controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor))
            {
                data.state = CAMQPDefinitionDeliveryState.decode(decoder);
            }
        }
        else
        {
            formatCode = decoder.readFormatCode();
            assert (formatCode == CAMQPFormatCodes.NULL);
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.resume = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetResume = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.more = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetMore = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.aborted = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetAborted = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.batchable = (formatCode == CAMQPFormatCodes.TRUE);
            data.isSetBatchable = true;
        }
        return data;
    }
}