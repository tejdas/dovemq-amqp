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

package net.dovemq.testutils;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.dovemq.transport.frame.CAMQPMessagePayload;

import static junit.framework.Assert.*;

public class CAMQPTestUtils
{
    public static void compareMaps(Map<String, String> inputMap, Map<String, String> outputMap)
    {
        assertTrue(outputMap.size() == inputMap.size());
        Set<String> keys = inputMap.keySet();
        for (String s : keys)
        {
            assertTrue(inputMap.get(s).equals(outputMap.get(s)));
        }
    }
    
    public static void compateByteArrays(byte[] input, byte[] output)
    {
        String inputStr = new String(input);
        String outputStr = new String(output);
        assertTrue(inputStr.equals(outputStr));
    }
    
    public static void compateByteArrayObjects(Object input, Object output)
    {
        assertTrue(input instanceof byte[]);
        assertTrue(output instanceof byte[]);
        
        String inputStr = new String((byte[]) input);
        String outputStr = new String((byte[]) output);
        assertTrue(inputStr.equals(outputStr));
    }
    
    public static void comparePayloads(CAMQPMessagePayload inputPayload, CAMQPMessagePayload outputPayload)
    {
        byte[] input = inputPayload.getPayload();
        byte[] output = outputPayload.getPayload();
        assertTrue(input != null);
        assertTrue(output != null);
        assertTrue(input.length == output.length);
        int size = input.length;
        for (int i = 0; i < size; i++)
        {
            assertTrue(input[i] == output[i]);
        }
    }
    
    public static void compateBigIntegerObjects(Object input, Object output)
    {
        assertTrue(input instanceof BigInteger);
        assertTrue(output instanceof BigInteger);
        
        BigInteger inputStr = (BigInteger) input;
        BigInteger outputStr = (BigInteger) output;       
        assertTrue(inputStr.longValue() ==  outputStr.longValue());
    }
    
    public static void compareStringObjects(Object input, Object output)
    {
        assertTrue(input instanceof String);
        assertTrue(output instanceof String);
        
        String inputStr = (String) input;
        String outputStr = (String) output;
        assertTrue(inputStr.equals(outputStr));
    }
    
    public static void compareUUIDObjects(Object input, Object output)
    {
        assertTrue(input instanceof UUID);
        assertTrue(output instanceof UUID);
        
        UUID inputStr = (UUID) input;
        UUID outputStr = (UUID) output;
        assertEquals(inputStr, outputStr);
    }
}
