package net.dovemq.transport.connection;

class CAMQPHeaderUtil
{
    static byte[] composeAMQPHeader()
    {
        byte[] header = new byte[CAMQPConnectionConstants.HEADER_LENGTH];
        header[0] = 'A';
        header[1] = 'M';
        header[2] = 'Q';
        header[3] = 'P';
        header[4] = CAMQPConnectionConstants.PROTOCOL_ID;        
        header[5] = CAMQPConnectionConstants.SUPPORTED_MAJOR_VERSION;
        header[6] = CAMQPConnectionConstants.SUPPORTED_MINOR_VERSION;        
        header[7] = CAMQPConnectionConstants.REVISION;
        return header;
    }

    static boolean validateAMQPHeader(byte[] amqpHeader)
    {
        return ((amqpHeader.length == CAMQPConnectionConstants.HEADER_LENGTH)
                && (amqpHeader[0] == 'A')
                && (amqpHeader[1] == 'M')
                && (amqpHeader[2] == 'Q')
                && (amqpHeader[3] == 'P')
                && (amqpHeader[4] == CAMQPConnectionConstants.PROTOCOL_ID)                
                && (amqpHeader[5] <= CAMQPConnectionConstants.SUPPORTED_MAJOR_VERSION)
                && (amqpHeader[6] <= CAMQPConnectionConstants.SUPPORTED_MINOR_VERSION)
                && (amqpHeader[7] <= CAMQPConnectionConstants.REVISION));
    }
}

