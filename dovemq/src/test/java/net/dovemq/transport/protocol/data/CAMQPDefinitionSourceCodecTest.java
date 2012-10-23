/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.protocol.data;

import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;

import static org.junit.Assert.assertTrue;

public class CAMQPDefinitionSourceCodecTest
{
    @Test
    public void testCAMQPDefinitionSource() throws Exception
    {
        CAMQPDefinitionSource data = new CAMQPDefinitionSource();
        data.setAddress("address");
        
        CAMQPDefinitionAccepted outcome = new CAMQPDefinitionAccepted();
        data.setDefaultOutcome(outcome);
        
        CAMQPDefinitionDeleteOnClose dynamicVal = new CAMQPDefinitionDeleteOnClose();
        dynamicVal.setRequiredOptions(true);
        dynamicVal.getOptions().put("dynoptkey", "dynoptval");
        data.setDynamic(true);

        data.setDistributionMode("distributionMode");

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

        assertTrue(((String) data.getAddress()).equalsIgnoreCase((String) outputData.getAddress()));
        assertTrue(data.getDefaultOutcome() instanceof CAMQPDefinitionAccepted);
        CAMQPDefinitionAccepted outcomeVal = (CAMQPDefinitionAccepted) data.getDefaultOutcome();
        assertTrue(!outcomeVal.isSetOptions());
        
        assertTrue(data.getDistributionMode().equalsIgnoreCase(outputData.getDistributionMode()));       
        assertTrue(data.getDynamic());
        
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
    
    @Test
    public void testCAMQPDefinitionSourceNoCapabilities() throws Exception
    {
        CAMQPDefinitionSource data = new CAMQPDefinitionSource();
        data.setAddress("address");
        data.setDistributionMode("distributionMode");
        
        CAMQPDefinitionAccepted outcome = new CAMQPDefinitionAccepted();
        data.setDefaultOutcome(outcome);
        
        CAMQPDefinitionDeleteOnClose dynamicVal = new CAMQPDefinitionDeleteOnClose();
        dynamicVal.setRequiredOptions(true);
        dynamicVal.getOptions().put("dynoptkey", "dynoptval");
        data.setDynamic(true);
        
        data.addCapabilities("capab1");
        data.addCapabilities("capab2");
        data.addCapabilities("capab3");
        data.setRequiredCapabilities(false);
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
        assertTrue(data.getDefaultOutcome() != null);
        assertTrue(data.getDistributionMode().equalsIgnoreCase(outputData.getDistributionMode()));
        assertTrue(data.getDynamic() != null);      
        
        assertTrue(outputData.getCapabilities().size() == 0);
        assertTrue(outputData.getOutcomes().size() == 4);
        assertTrue(outputData.getOutcomes().containsAll(data.getOutcomes()));        
    }
    
    @Test
    public void testCAMQPDefinitionSourceNoOutcomes() throws Exception
    {
        CAMQPDefinitionSource data = new CAMQPDefinitionSource();
        data.setAddress("address");
        data.setDistributionMode("distributionMode");
        
        CAMQPDefinitionAccepted outcome = new CAMQPDefinitionAccepted();
        data.setDefaultOutcome(outcome);
        
        CAMQPDefinitionDeleteOnClose dynamicVal = new CAMQPDefinitionDeleteOnClose();
        dynamicVal.setRequiredOptions(true);
        dynamicVal.getOptions().put("dynoptkey", "dynoptval");
        data.setDynamic(true);
        
        
        data.addCapabilities("capab1");
        data.addCapabilities("capab2");
        data.addCapabilities("capab3");
        data.addOutcomes("outcomes1");
        data.addOutcomes("outcomes2");
        data.addOutcomes("outcomes3");
        data.addOutcomes("outcomes4");
        data.setRequiredOutcomes(false);
        
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
 
        assertTrue(data.getDistributionMode().equalsIgnoreCase(outputData.getDistributionMode()));
        
        assertTrue(outputData.getCapabilities().containsAll(data.getCapabilities()));
        assertTrue(outputData.getOutcomes().size() == 0);        
    }
    
    @Test
    public void testCAMQPDefinitionSourceNoFilter() throws Exception
    {
        CAMQPDefinitionSource data = new CAMQPDefinitionSource();
        data.setAddress("address");
        
        data.setDefaultOutcome("defaultOutcome");
        data.setRequiredDefaultOutcome(false);
        data.setDynamic(true);
        data.setRequiredDynamic(false);
        data.setDistributionMode("distributionMode");

        data.getFilter().put("opt1", "val1");
        data.getFilter().put("opt2", "val2");
        data.getFilter().put("opt3", "val3");
        data.setRequiredFilter(false);
        
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
        
        assertTrue(data.getDistributionMode().equalsIgnoreCase(outputData.getDistributionMode()));   
        assertTrue(outputData.getCapabilities().containsAll(data.getCapabilities()));
        assertTrue(outputData.getOutcomes().containsAll(data.getOutcomes()));
        assertTrue(outputData.getFilter().size() == 0);
    }
}
