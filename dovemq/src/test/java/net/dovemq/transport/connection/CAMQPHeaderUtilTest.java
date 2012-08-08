package net.dovemq.transport.connection;

import junit.framework.TestCase;

public class CAMQPHeaderUtilTest extends TestCase
{

    public CAMQPHeaderUtilTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testAMQPHeader()
    {
        {
            byte[] amqpHeader = CAMQPHeaderUtil.composeAMQPHeader();
            assertTrue(CAMQPHeaderUtil.validateAMQPHeader(amqpHeader));
        }
        
        {
            byte[] amqpHeader = CAMQPHeaderUtil.composeAMQPHeader();
            amqpHeader[6] = 1;
            amqpHeader[7] = 1;
            assertFalse(CAMQPHeaderUtil.validateAMQPHeader(amqpHeader));
        }      
        
        {
            byte[] amqpHeader = new byte[] {'H', 'T', 'T', 'P', 0, 1, 0, 0};
            assertFalse(CAMQPHeaderUtil.validateAMQPHeader(amqpHeader));
        }        
    }
}
