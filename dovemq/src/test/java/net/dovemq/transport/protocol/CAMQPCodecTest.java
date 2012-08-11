package net.dovemq.transport.protocol;

import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlOpen;
import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;
import net.dovemq.transport.protocol.data.CAMQPDefinitionDeleteOnClose;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.protocol.data.CAMQPDefinitionRejected;
import net.dovemq.transport.protocol.data.CAMQPDefinitionSource;

import junit.framework.TestCase;

public class CAMQPCodecTest extends TestCase
{
    public CAMQPCodecTest(String name)
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
    
    public void testCAMQPControlBeginCodec() throws Exception
    {
        CAMQPControlBegin data = new CAMQPControlBegin();
        data.setRemoteChannel(23);
        
        data.getProperties().put("prop1", "propval1");
        data.getProperties().put("prop2", "propval2");
        data.getProperties().put("prop3", "propval3");
        
        data.addDesiredCapabilities("desired-cap1");
        data.addDesiredCapabilities("desired-cap2");
        data.addDesiredCapabilities("desired-cap3");
        
        data.addOfferedCapabilities("offered-cap1");
        data.addOfferedCapabilities("offered-cap2");
        data.addOfferedCapabilities("offered-cap3");   
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlBegin.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlBegin.descriptor));        
        CAMQPControlBegin outputData = CAMQPControlBegin.decode(inputPipe);
        
        assertTrue(outputData.getRemoteChannel() == data.getRemoteChannel());
        
        {
            Map<String, String> map = outputData.getProperties();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outputData.getProperties().get(s).equalsIgnoreCase(data.getProperties().get(s)));                
            }
        }  

        assertTrue(outputData.getOfferedCapabilities().containsAll(data.getOfferedCapabilities()));
        assertTrue(outputData.getDesiredCapabilities().containsAll(data.getDesiredCapabilities()));    
    }
    
    public void testCAMQPControlBeginCodecNoOfferedCapability() throws Exception
    {
        CAMQPControlBegin data = new CAMQPControlBegin();
        data.setRemoteChannel(23);
        
        data.getProperties().put("prop1", "propval1");
        data.getProperties().put("prop2", "propval2");
        data.getProperties().put("prop3", "propval3");
        
        data.addDesiredCapabilities("desired-cap1");
        data.addDesiredCapabilities("desired-cap2");
        data.addDesiredCapabilities("desired-cap3");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlBegin.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlBegin.descriptor));        
        CAMQPControlBegin outputData = CAMQPControlBegin.decode(inputPipe);
        
        assertTrue(outputData.getRemoteChannel() == data.getRemoteChannel());
        
        {
            Map<String, String> map = outputData.getProperties();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outputData.getProperties().get(s).equalsIgnoreCase(data.getProperties().get(s)));                
            }
        }  

        assertTrue(outputData.getOfferedCapabilities().isEmpty());
        assertTrue(outputData.getDesiredCapabilities().containsAll(data.getDesiredCapabilities()));        
    }    
    
    public void testCAMQPControlBeginCodecOptionalOfferedCapability() throws Exception
    {
        CAMQPControlBegin data = new CAMQPControlBegin();
        data.setRemoteChannel(23);

        data.getProperties().put("prop1", "propval1");
        data.getProperties().put("prop2", "propval2");
        data.getProperties().put("prop3", "propval3");
        
        data.addDesiredCapabilities("desired-cap1");
        data.addDesiredCapabilities("desired-cap2");
        data.addDesiredCapabilities("desired-cap3");
        
        data.addOfferedCapabilities("offered-cap1");
        data.addOfferedCapabilities("offered-cap2");
        data.addOfferedCapabilities("offered-cap3");        
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlBegin.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlBegin.descriptor));        
        CAMQPControlBegin outputData = CAMQPControlBegin.decode(inputPipe);
        
        assertTrue(outputData.getRemoteChannel() == data.getRemoteChannel());
 
        {
            Map<String, String> map = outputData.getProperties();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outputData.getProperties().get(s).equalsIgnoreCase(data.getProperties().get(s)));                
            }
        }  

        assertTrue(outputData.getOfferedCapabilities().containsAll(data.getOfferedCapabilities()));
        assertTrue(outputData.getDesiredCapabilities().containsAll(data.getDesiredCapabilities()));        
    }

    public void testCAMQPControlOpenCodec() throws Exception
    {
        CAMQPControlOpen data = new CAMQPControlOpen();
        
        data.setChannelMax(64);
        data.setContainerId("amqp-broker");
        data.setIdleTimeOut(4000L);
        data.setHostname("tejdas-win2003");
        data.setMaxFrameSize(65536L);
        
        data.addDesiredCapabilities("desired-cap1");
        data.addDesiredCapabilities("desired-cap2");
        data.addDesiredCapabilities("desired-cap3");
        
        data.addOfferedCapabilities("offered-cap1");
        data.addOfferedCapabilities("offered-cap2");
        data.addOfferedCapabilities("offered-cap3");
        
        data.addIncomingLocales("English");
        data.addIncomingLocales("French");        
        
        data.addOutgoingLocales("Hindi");
        data.addOutgoingLocales("Hebrew");
        
        data.getProperties().put("prop1", "propval1");
        data.getProperties().put("prop2", "propval2");
        data.getProperties().put("prop3", "propval3");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlOpen.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPControlOpen.descriptor));        
        CAMQPControlOpen outputData = CAMQPControlOpen.decode(inputPipe);
        
        assertTrue(outputData.getContainerId().equalsIgnoreCase(data.getContainerId()));
        assertTrue(outputData.getHostname().equalsIgnoreCase(data.getHostname()));        
        assertTrue(outputData.getChannelMax() == data.getChannelMax());
        assertEquals(outputData.getIdleTimeOut(), data.getIdleTimeOut());
        assertEquals(outputData.getMaxFrameSize(), data.getMaxFrameSize());        
        
        {
            Map<String, String> map = outputData.getProperties();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outputData.getProperties().get(s).equalsIgnoreCase(data.getProperties().get(s)));                
            }
        }  

        assertTrue(outputData.getOfferedCapabilities().containsAll(data.getOfferedCapabilities()));
        assertTrue(outputData.getDesiredCapabilities().containsAll(data.getDesiredCapabilities()));        
        assertTrue(outputData.getIncomingLocales().containsAll(data.getIncomingLocales()));
        assertTrue(outputData.getOutgoingLocales().containsAll(data.getOutgoingLocales()));        
    }
    
    public void testCAMQPDefinitionRejected() throws Exception
    {
        CAMQPDefinitionRejected data = new CAMQPDefinitionRejected();
        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");
        errorInfo.getInfo().put("key1", "val1");
        errorInfo.getInfo().put("key2", "val2");
        data.setError(errorInfo);
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionRejected.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionRejected.descriptor));
        CAMQPDefinitionRejected outputData = CAMQPDefinitionRejected.decode(inputPipe);
        assertTrue(outputData.getError().getCondition().equalsIgnoreCase(data.getError().getCondition()));
        assertTrue(outputData.getError().getDescription().equalsIgnoreCase(data.getError().getDescription()));
        
        {
            Map<String, String> map = outputData.getError().getInfo();
            Set<String> keys = map.keySet();
            
            for (String s : keys)
            {
                assertTrue(outputData.getError().getInfo().get(s).equalsIgnoreCase(data.getError().getInfo().get(s)));                
            }
        }        
    }
    
    public void testCAMQPDefinitionRejectedNoDescription() throws Exception
    {
        CAMQPDefinitionRejected data = new CAMQPDefinitionRejected();
        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");
        errorInfo.getInfo().put("key1", "val1");
        errorInfo.getInfo().put("key2", "val2");
        data.setError(errorInfo);
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionRejected.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionRejected.descriptor));
        CAMQPDefinitionRejected outputData = CAMQPDefinitionRejected.decode(inputPipe);
        assertTrue(outputData.getError().getCondition().equalsIgnoreCase(data.getError().getCondition()));
        assertTrue(outputData.getError().getDescription().equalsIgnoreCase(data.getError().getDescription()));
        
        {
            Map<String, String> map = outputData.getError().getInfo();
            Set<String> keys = map.keySet();
            
            for (String s : keys)
            {
                assertTrue(outputData.getError().getInfo().get(s).equalsIgnoreCase(data.getError().getInfo().get(s)));                
            }
        }        
    }
    
    public void testCAMQPDefinitionRejectedErrorNoInfo() throws Exception
    {
        CAMQPDefinitionRejected data = new CAMQPDefinitionRejected();
        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");
        errorInfo.setRequiredInfo(true);
        errorInfo.getInfo().put("key1", "val1");
        errorInfo.getInfo().put("key2", "val2");
        data.setError(errorInfo);
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionRejected.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionRejected.descriptor));
        CAMQPDefinitionRejected outputData = CAMQPDefinitionRejected.decode(inputPipe);
        assertTrue(outputData.getError().getCondition().equalsIgnoreCase(data.getError().getCondition()));
        assertTrue(outputData.getError().getDescription().equalsIgnoreCase(data.getError().getDescription()));
        
        {
            Map<String, String> map = outputData.getError().getInfo();
            assertTrue(map.size() == 2);
            Set<String> keys = map.keySet();
            
            for (String s : keys)
            {
                assertTrue(outputData.getError().getInfo().get(s).equalsIgnoreCase(outputData.getError().getInfo().get(s)));                
            }
        }
    }
    
    public void testCAMQPDefinitionRejectedNoErrorInfo() throws Exception
    {
        CAMQPDefinitionRejected data = new CAMQPDefinitionRejected();
        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");
        errorInfo.setRequiredInfo(true);
        errorInfo.getInfo().put("key1", "val1");
        errorInfo.getInfo().put("key2", "val2");
        data.setError(errorInfo);
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionRejected.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionRejected.descriptor));
        CAMQPDefinitionRejected outputData = CAMQPDefinitionRejected.decode(inputPipe);
        assertTrue(outputData.getError() != null);
        assertTrue(outputData.getError().getCondition().equalsIgnoreCase("testErrorCondition"));
        assertTrue(outputData.getError().getDescription().equalsIgnoreCase("testErrorDescription"));
        assertTrue(outputData.getError().getInfo().size() == 2);
    }    
    
    public void testCAMQPDefinitionError() throws Exception
    {
        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionError.encode(outstream, errorInfo);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionError.descriptor));
        CAMQPDefinitionError outputData = CAMQPDefinitionError.decode(inputPipe);
        
        assertTrue(outputData.getCondition().equalsIgnoreCase(errorInfo.getCondition()));
        assertTrue(outputData.getDescription().equalsIgnoreCase(errorInfo.getDescription()));        
    }
    
    public void testCAMQPDefinitionSourceNo() throws Exception
    {
        CAMQPDefinitionSource data = new CAMQPDefinitionSource();
        data.setAddress("address");
        
        CAMQPDefinitionAccepted defAccepted = new CAMQPDefinitionAccepted();
        data.setDefaultOutcome(defAccepted);
        
        data.setDistributionMode("distributionMode");
        
        data.setDynamic(true);

        data.getFilter().put("opt1", "val1");
        data.getFilter().put("opt2", "val2");
        data.getFilter().put("opt3", "val3");
        
        data.addCapabilities("capab1");
        data.addCapabilities("capab2");
        data.addCapabilities("capab3");
        data.addOutcomes("outcomes1");
        data.addOutcomes("outcomes2");
        data.addOutcomes("outcomes3");
        data.addOutcomes("outcomes4");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionSource.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionSource.descriptor));
        CAMQPDefinitionSource outputData = CAMQPDefinitionSource.decode(inputPipe);

        String inAddr = (String) data.getAddress();
        String outAddr = (String) outputData.getAddress();
        assertTrue(inAddr.equalsIgnoreCase(outAddr));
        assertTrue(outputData.getDistributionMode().equalsIgnoreCase("distributionMode"));
        assertTrue(outputData.getDynamic());     
        
        assertTrue(outputData.getCapabilities().containsAll(data.getCapabilities()));
        assertTrue(outputData.getOutcomes().containsAll(data.getOutcomes()));
        
        {
            Map<String, String> map = outputData.getFilter();
            Set<String> keys = map.keySet();
            
            for (String s : keys)
            {
                assertTrue(outputData.getFilter().get(s).equalsIgnoreCase(data.getFilter().get(s)));                
            }
        }               
    }    
}
