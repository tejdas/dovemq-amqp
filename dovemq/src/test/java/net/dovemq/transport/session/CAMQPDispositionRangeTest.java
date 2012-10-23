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

package net.dovemq.transport.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;
import net.dovemq.transport.protocol.data.CAMQPDefinitionModified;
import net.dovemq.transport.protocol.data.CAMQPDefinitionRejected;
import net.dovemq.transport.session.CAMQPDispositionSender.DispositionRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class CAMQPDispositionRangeTest
{
    @Before
    public void setup()
    {
    }

    @After
    public void tearDown()
    {
    }
    
    @Test
    public void testDispositionRangeCompatibility()
    {
        DispositionRange dispRange = new DispositionRange(1, 1, false, null);
        
        assertTrue(dispRange.isCompatible(false,  null));
        assertTrue(!dispRange.isCompatible(true,  null));
        assertTrue(!dispRange.isCompatible(false, new CAMQPDefinitionAccepted()));
        
        CAMQPDefinitionAccepted outcome = new CAMQPDefinitionAccepted();
        dispRange = new DispositionRange(1, 1, false, outcome);
        assertTrue(dispRange.isCompatible(false, outcome));
        assertTrue(dispRange.isCompatible(false, new CAMQPDefinitionAccepted()));
        assertTrue(!dispRange.isCompatible(false, new CAMQPDefinitionModified()));
        assertTrue(!dispRange.isCompatible(false, new CAMQPDefinitionRejected()));        
        
        dispRange = new DispositionRange(1, 1, true, new CAMQPDefinitionRejected());
        assertTrue(dispRange.isCompatible(true, new CAMQPDefinitionRejected())); 
    }
    
    @Test
    public void testDispositionRange() throws IOException
    {
        List<DispositionRange> dispositionRanges = new LinkedList<DispositionRange>();
        InputStream ins = getClass().getClassLoader().getResourceAsStream("input.txt");
        
        BufferedReader fis = new BufferedReader(new InputStreamReader(ins));
        String str;
        while ((str = fis.readLine()) != null)
        {
            String[] argList = null;
            if (str.indexOf(",") != -1) {
                argList = str.split(",");
                String valstr = argList[0];
                String settledstr = argList[1];
                String outcomeStr = argList[2];
                
                long val = Long.valueOf(valstr);
                boolean settled = Boolean.valueOf(settledstr);
                
                Object outcome = null;
                if (outcomeStr.equalsIgnoreCase("a"))
                    outcome = new CAMQPDefinitionAccepted();
                else if (outcomeStr.equalsIgnoreCase("m"))
                    outcome = new CAMQPDefinitionModified();
                else if (outcomeStr.equalsIgnoreCase("r"))
                    outcome = new CAMQPDefinitionRejected();
                
                CAMQPDispositionSender.addDisposition(val, settled, outcome, dispositionRanges);
            }
        }
        for (DispositionRange range : dispositionRanges)
        {
            System.out.println(range.toString());
        }
        fis.close();
    }
}
