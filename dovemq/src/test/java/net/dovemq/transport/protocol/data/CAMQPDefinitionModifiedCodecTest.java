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

import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPDefinitionModified;

import junit.framework.TestCase;

public class CAMQPDefinitionModifiedCodecTest extends TestCase
{

    public CAMQPDefinitionModifiedCodecTest(String name)
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
    
    
    public void testCAMQPDefinitionModifiedCodec() throws Exception
    {
        CAMQPDefinitionModified outcomeVal = new CAMQPDefinitionModified();
        outcomeVal.setUndeliverableHere(true);
        outcomeVal.setDeliveryFailed(true);
        outcomeVal.setRequiredMessageAttrs(true);
        outcomeVal.getMessageAttrs().put("msgkey", "msgval");
        
        CAMQPEncoder outstream = CAMQPEncoder.createCAMQPEncoder();
        CAMQPDefinitionModified.encode(outstream, outcomeVal);
        ChannelBuffer buffer = outstream.getEncodedBuffer();
        CAMQPSyncDecoder inputPipe =
                CAMQPSyncDecoder.createCAMQPSyncDecoder();
        inputPipe.take(buffer);
        
        String controlName = inputPipe.readSymbol();
        assertTrue(controlName.equalsIgnoreCase(CAMQPDefinitionModified.descriptor));
        CAMQPDefinitionModified outcomeValOut = CAMQPDefinitionModified.decode(inputPipe);
        
        assertTrue(outcomeVal.getUndeliverableHere() == outcomeValOut.getUndeliverableHere());
        assertTrue(outcomeVal.getDeliveryFailed() == outcomeValOut.getDeliveryFailed());      
        
        {
            assertTrue(outcomeValOut.getMessageAttrs().size() == outcomeVal.getMessageAttrs().size());
            Map<String, String> map = outcomeValOut.getMessageAttrs();
            Set<String> keys = map.keySet();
            for (String s : keys)
            {
                assertTrue(outcomeVal.getMessageAttrs().get(s).equalsIgnoreCase(outcomeValOut.getMessageAttrs().get(s)));                
            }
        }        
    }
}
