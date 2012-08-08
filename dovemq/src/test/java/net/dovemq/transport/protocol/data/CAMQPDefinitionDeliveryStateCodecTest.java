package net.dovemq.transport.protocol.data;

import java.math.BigInteger;

import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.protocol.data.CAMQPDefinitionModified;
import net.dovemq.transport.protocol.data.CAMQPDefinitionRejected;
import net.dovemq.transport.protocol.data.CAMQPDefinitionReleased;

import junit.framework.TestCase;

public class CAMQPDefinitionDeliveryStateCodecTest extends TestCase
{

    public CAMQPDefinitionDeliveryStateCodecTest(String name)
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
    
    public void testCAMQPDefinitionDeliveryStateCodec() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        data.setSectionOffset(BigInteger.valueOf(bytesTransferred));

        CAMQPDefinitionModified outcomeVal = new CAMQPDefinitionModified();
        outcomeVal.setUndeliverableHere(false);
        outcomeVal.setDeliveryFailed(false);
        //outcomeVal.getDeliveryAttrs().put("deliverkey", "delivervalue");
        outcomeVal.getMessageAttrs().put("msgkey", "msgval");
        data.setOutcome(outcomeVal);
        data.setTxnId("transactionid4");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        long bytesTransferredOut =  outputData.getSectionOffset().longValue();
        assertEquals(bytesTransferred, bytesTransferredOut);
        
        CAMQPDefinitionModified outcomeValOut = (CAMQPDefinitionModified) outputData.getOutcome(); 
        assertTrue(outcomeVal.getUndeliverableHere() == outcomeValOut.getUndeliverableHere());
        assertTrue(outcomeVal.getDeliveryFailed() == outcomeValOut.getDeliveryFailed());
        assertTrue(data.getTxnId().equalsIgnoreCase(outputData.getTxnId()));        
    }
    
    public void testCAMQPDefinitionDeliveryStateCodecNoBytesTransferred() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        
        CAMQPDefinitionAccepted outcomeVal = new CAMQPDefinitionAccepted();
        data.setOutcome(outcomeVal);
        data.setTxnId("transactionid4");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        assertTrue(outputData.getSectionOffset() == null);
        assertTrue(outputData.getOutcome() != null);
        assertTrue(data.getTxnId().equalsIgnoreCase(outputData.getTxnId()));        
    }
    
    public void testCAMQPDefinitionDeliveryStateCodecNoOutcome() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        data.setSectionOffset(BigInteger.valueOf(bytesTransferred));
        data.setTxnId("transactionid4");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        long bytesTransferredOut =  outputData.getSectionOffset().longValue();
        assertEquals(bytesTransferred, bytesTransferredOut);
        assertTrue(outputData.getOutcome() == null);
        assertTrue(data.getTxnId().equalsIgnoreCase(outputData.getTxnId()));        
    }
    
    public void testCAMQPDefinitionDeliveryStateCodecNoTxnId() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        data.setSectionOffset(BigInteger.valueOf(bytesTransferred));

        CAMQPDefinitionError errorInfo = new CAMQPDefinitionError();
        errorInfo.setCondition("testErrorCondition");
        errorInfo.setDescription("testErrorDescription");        
        CAMQPDefinitionRejected outCome = new CAMQPDefinitionRejected();
        outCome.setError(errorInfo);
        data.setOutcome(outCome);
        
        data.setTxnId("transactionid4");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        long bytesTransferredOut =  outputData.getSectionOffset().longValue();
        assertEquals(bytesTransferred, bytesTransferredOut);

        assertTrue(outputData.getOutcome() != null);
        CAMQPDefinitionRejected decodedOutcome = (CAMQPDefinitionRejected) outputData.getOutcome(); 
        CAMQPDefinitionError outError = decodedOutcome.getError();
        assertTrue(outError.getCondition().equalsIgnoreCase(errorInfo.getCondition()));
        assertTrue(outError.getDescription().equalsIgnoreCase(errorInfo.getDescription()));       
        assertTrue(outputData.getTxnId().equalsIgnoreCase("transactionid4"));        
    }
    
    public void testCAMQPDefinitionDeliveryStateOutcomeReleaseCodecNoTxn() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        data.setSectionOffset(BigInteger.valueOf(bytesTransferred));

        CAMQPDefinitionReleased outcomeVal = new CAMQPDefinitionReleased();
        data.setOutcome(outcomeVal);
      
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        long bytesTransferredOut =  outputData.getSectionOffset().longValue();
        assertEquals(bytesTransferred, bytesTransferredOut);
        
        CAMQPDefinitionReleased outcomeValOut = (CAMQPDefinitionReleased) outputData.getOutcome();
        assertTrue(outcomeValOut != null);
        assertTrue(outputData.getTxnId() == null);        
    } 
    
    public void testCAMQPDefinitionDeliveryStateOutcomeReleaseCodec() throws Exception
    {
        CAMQPDefinitionDeliveryState data = new CAMQPDefinitionDeliveryState();
        long bytesTransferred = 87467L;
        data.setSectionOffset(BigInteger.valueOf(bytesTransferred));

        CAMQPDefinitionReleased outcomeVal = new CAMQPDefinitionReleased();
        data.setOutcome(outcomeVal);
        
        data.setTxnId("transactionID5");
      
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionDeliveryState.encode(outstream, data);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionDeliveryState.descriptor));
        CAMQPDefinitionDeliveryState outputData = CAMQPDefinitionDeliveryState.decode(inputPipe);
        long bytesTransferredOut =  outputData.getSectionOffset().longValue();
        assertEquals(bytesTransferred, bytesTransferredOut);
        
        CAMQPDefinitionReleased outcomeValOut = (CAMQPDefinitionReleased) outputData.getOutcome();
        assertTrue(outcomeValOut != null);
        assertTrue(outputData.getTxnId().equalsIgnoreCase(data.getTxnId()));        
    }    
}
