package net.dovemq.transport.protocol.data;

import java.util.Date;
import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.*;

public class CAMQPDefinitionProperties
{
    public static final String descriptor = "amqp:properties:list";

    private boolean isSetMessageId = false;
    public void setRequiredMessageId(boolean val)
    {
        isSetMessageId = val;
    }
    public boolean isSetMessageId()
    {
        return isSetMessageId;
    }
    private Object messageId = null;
    public void setMessageId(Object val)
    {
        isSetMessageId = true;
        messageId = val;
    }

    public Object getMessageId()
    {
        return messageId;
    }

    private boolean isSetUserId = false;
    public void setRequiredUserId(boolean val)
    {
        isSetUserId = val;
    }
    public boolean isSetUserId()
    {
        return isSetUserId;
    }
    private byte[] userId = null;
    public void setUserId(byte[] val)
    {
        isSetUserId = true;
        userId = val;
    }

    public byte[] getUserId()
    {
        return userId;
    }

    private boolean isSetTo = false;
    public void setRequiredTo(boolean val)
    {
        isSetTo = val;
    }
    public boolean isSetTo()
    {
        return isSetTo;
    }
    private Object to = null;
    public void setTo(Object val)
    {
        isSetTo = true;
        to = val;
    }

    public Object getTo()
    {
        return to;
    }

    private boolean isSetSubject = false;
    public void setRequiredSubject(boolean val)
    {
        isSetSubject = val;
    }
    public boolean isSetSubject()
    {
        return isSetSubject;
    }
    private String subject = null;
    public void setSubject(String val)
    {
        isSetSubject = true;
        subject = val;
    }

    public String getSubject()
    {
        return subject;
    }

    private boolean isSetReplyTo = false;
    public void setRequiredReplyTo(boolean val)
    {
        isSetReplyTo = val;
    }
    public boolean isSetReplyTo()
    {
        return isSetReplyTo;
    }
    private Object replyTo = null;
    public void setReplyTo(Object val)
    {
        isSetReplyTo = true;
        replyTo = val;
    }

    public Object getReplyTo()
    {
        return replyTo;
    }

    private boolean isSetCorrelationId = false;
    public void setRequiredCorrelationId(boolean val)
    {
        isSetCorrelationId = val;
    }
    public boolean isSetCorrelationId()
    {
        return isSetCorrelationId;
    }
    private Object correlationId = null;
    public void setCorrelationId(Object val)
    {
        isSetCorrelationId = true;
        correlationId = val;
    }

    public Object getCorrelationId()
    {
        return correlationId;
    }

    private boolean isSetContentType = false;
    public void setRequiredContentType(boolean val)
    {
        isSetContentType = val;
    }
    public boolean isSetContentType()
    {
        return isSetContentType;
    }
    private String contentType = null;
    public void setContentType(String val)
    {
        isSetContentType = true;
        contentType = val;
    }

    public String getContentType()
    {
        return contentType;
    }

    private boolean isSetContentEncoding = false;
    public void setRequiredContentEncoding(boolean val)
    {
        isSetContentEncoding = val;
    }
    public boolean isSetContentEncoding()
    {
        return isSetContentEncoding;
    }
    private String contentEncoding = null;
    public void setContentEncoding(String val)
    {
        isSetContentEncoding = true;
        contentEncoding = val;
    }

    public String getContentEncoding()
    {
        return contentEncoding;
    }

    private boolean isSetAbsoluteExpiryTime = false;
    public void setRequiredAbsoluteExpiryTime(boolean val)
    {
        isSetAbsoluteExpiryTime = val;
    }
    public boolean isSetAbsoluteExpiryTime()
    {
        return isSetAbsoluteExpiryTime;
    }
    private Date absoluteExpiryTime = null;
    public void setAbsoluteExpiryTime(Date val)
    {
        isSetAbsoluteExpiryTime = true;
        absoluteExpiryTime = val;
    }

    public Date getAbsoluteExpiryTime()
    {
        return absoluteExpiryTime;
    }

    private boolean isSetCreationTime = false;
    public void setRequiredCreationTime(boolean val)
    {
        isSetCreationTime = val;
    }
    public boolean isSetCreationTime()
    {
        return isSetCreationTime;
    }
    private Date creationTime = null;
    public void setCreationTime(Date val)
    {
        isSetCreationTime = true;
        creationTime = val;
    }

    public Date getCreationTime()
    {
        return creationTime;
    }

    private boolean isSetGroupId = false;
    public void setRequiredGroupId(boolean val)
    {
        isSetGroupId = val;
    }
    public boolean isSetGroupId()
    {
        return isSetGroupId;
    }
    private String groupId = null;
    public void setGroupId(String val)
    {
        isSetGroupId = true;
        groupId = val;
    }

    public String getGroupId()
    {
        return groupId;
    }

    private boolean isSetGroupSequence = false;
    public void setRequiredGroupSequence(boolean val)
    {
        isSetGroupSequence = val;
    }
    public boolean isSetGroupSequence()
    {
        return isSetGroupSequence;
    }
    private Long groupSequence = 0L;
    public void setGroupSequence(Long val)
    {
        isSetGroupSequence = true;
        groupSequence = val;
    }

    public Long getGroupSequence()
    {
        return groupSequence;
    }

    private boolean isSetReplyToGroupId = false;
    public void setRequiredReplyToGroupId(boolean val)
    {
        isSetReplyToGroupId = val;
    }
    public boolean isSetReplyToGroupId()
    {
        return isSetReplyToGroupId;
    }
    private String replyToGroupId = null;
    public void setReplyToGroupId(String val)
    {
        isSetReplyToGroupId = true;
        replyToGroupId = val;
    }

    public String getReplyToGroupId()
    {
        return replyToGroupId;
    }

    public static void encode(CAMQPEncoder encoder, CAMQPDefinitionProperties data)
    {
        long listSize = 13;
        encoder.writeListDescriptor(descriptor, listSize);

        if ((data.messageId != null) && (data.isSetMessageId))
        {
            if (data.messageId instanceof java.util.UUID)
            {
                encoder.writeUUID((java.util.UUID) data.messageId);
            }
            else if (data.messageId instanceof java.math.BigInteger)
            {
                encoder.writeULong((java.math.BigInteger) data.messageId);
            }
            else if (data.messageId instanceof byte[])
            {
                byte[] binData = (byte[]) data.messageId;
                encoder.writeBinary(binData, binData.length, false);
            }
            else if (data.messageId instanceof String)
            {
                encoder.writeUTF8String((String) data.messageId);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.userId != null) && (data.isSetUserId))
        {
            encoder.writeBinary(data.userId, data.userId.length, false);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.to != null) && (data.isSetTo))
        {
            if (data.to instanceof String)
            {
                encoder.writeUTF8String((String) data.to);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.subject != null) && (data.isSetSubject))
        {
            encoder.writeUTF8String(data.subject);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.replyTo != null) && (data.isSetReplyTo))
        {
            if (data.replyTo instanceof String)
            {
                encoder.writeUTF8String((String) data.replyTo);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.correlationId != null) && (data.isSetCorrelationId))
        {
            if (data.correlationId instanceof java.util.UUID)
            {
                encoder.writeUUID((java.util.UUID) data.correlationId);
            }
            else if (data.correlationId instanceof java.math.BigInteger)
            {
                encoder.writeULong((java.math.BigInteger) data.correlationId);
            }
            else if (data.correlationId instanceof byte[])
            {
                byte[] binData = (byte[]) data.correlationId;
                encoder.writeBinary(binData, binData.length, false);
            }
            else if (data.correlationId instanceof String)
            {
                encoder.writeUTF8String((String) data.correlationId);
            }
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.contentType != null) && (data.isSetContentType))
        {
            encoder.writeSymbol(data.contentType);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.contentEncoding != null) && (data.isSetContentEncoding))
        {
            encoder.writeSymbol(data.contentEncoding);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetAbsoluteExpiryTime)
        {
            encoder.writeTimeStamp(data.absoluteExpiryTime);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetCreationTime)
        {
            encoder.writeTimeStamp(data.creationTime);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.groupId != null) && (data.isSetGroupId))
        {
            encoder.writeUTF8String(data.groupId);
        }
        else
        {
            encoder.writeNull();
        }

        if (data.isSetGroupSequence)
        {
            encoder.writeUInt(data.groupSequence);
        }
        else
        {
            encoder.writeNull();
        }

        if ((data.replyToGroupId != null) && (data.isSetReplyToGroupId))
        {
            encoder.writeUTF8String(data.replyToGroupId);
        }
        else
        {
            encoder.writeNull();
        }
        encoder.fillCompoundSize(listSize);
    }
    public static CAMQPDefinitionProperties decode(CAMQPSyncDecoder decoder)
    {
        int formatCode;
        formatCode = decoder.readFormatCode();
        assert((formatCode == CAMQPFormatCodes.LIST8) || (formatCode == CAMQPFormatCodes.LIST32));

        long listSize = decoder.readCompoundSize(formatCode);
        assert(listSize == 13);
        CAMQPDefinitionProperties data = new CAMQPDefinitionProperties();

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            if (formatCode == CAMQPFormatCodes.UUID)
            {
                data.messageId = decoder.readUUID();
            }
            else if (formatCode == CAMQPFormatCodes.LONG)
            {
                data.messageId = decoder.readULong();
            }
            else if (formatCode == CAMQPFormatCodes.VBIN8)
            {
                int size = (int) decoder.readBinaryDataSize(formatCode);
                ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
                data.messageId = new byte[size];
                channelBuf.readBytes((byte[]) data.messageId);
            }
            else if (formatCode == CAMQPFormatCodes.STR8_UTF8)
            {
                data.messageId = decoder.readString(formatCode);
            }
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            int size = (int) decoder.readBinaryDataSize(formatCode);
            ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
            data.userId = new byte[size];
            channelBuf.readBytes(data.userId);
            data.isSetUserId = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            if (formatCode == CAMQPFormatCodes.STR8_UTF8)
            {
                data.to = decoder.readString(formatCode);
            }
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.subject = decoder.readString(formatCode);
            data.isSetSubject = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            if (formatCode == CAMQPFormatCodes.STR8_UTF8)
            {
                data.replyTo = decoder.readString(formatCode);
            }
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            if (formatCode == CAMQPFormatCodes.UUID)
            {
                data.correlationId = decoder.readUUID();
            }
            else if (formatCode == CAMQPFormatCodes.LONG)
            {
                data.correlationId = decoder.readULong();
            }
            else if (formatCode == CAMQPFormatCodes.VBIN8)
            {
                int size = (int) decoder.readBinaryDataSize(formatCode);
                ChannelBuffer channelBuf = decoder.readBinary(formatCode, size, false);
                data.correlationId = new byte[size];
                channelBuf.readBytes((byte[]) data.correlationId);
            }
            else if (formatCode == CAMQPFormatCodes.STR8_UTF8)
            {
                data.correlationId = decoder.readString(formatCode);
            }
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.contentType = decoder.readString(formatCode);
            data.isSetContentType = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.contentEncoding = decoder.readString(formatCode);
            data.isSetContentEncoding = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.absoluteExpiryTime = decoder.readTimeStamp();
            data.isSetAbsoluteExpiryTime = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.creationTime = decoder.readTimeStamp();
            data.isSetCreationTime = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.groupId = decoder.readString(formatCode);
            data.isSetGroupId = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.groupSequence = decoder.readUInt();
            data.isSetGroupSequence = true;
        }

        formatCode = decoder.readFormatCode();
        if (formatCode != CAMQPFormatCodes.NULL)
        {
            data.replyToGroupId = decoder.readString(formatCode);
            data.isSetReplyToGroupId = true;
        }
        return data;
    }
}
