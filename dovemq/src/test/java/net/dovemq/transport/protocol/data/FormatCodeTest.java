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

import org.apache.log4j.Logger;
import org.junit.Test;

import net.dovemq.transport.protocol.CAMQPCodecUtil;
import net.dovemq.transport.protocol.data.CAMQPTypes;

import junit.framework.TestCase;

public class FormatCodeTest extends TestCase
{
    private static Logger log = Logger.getLogger(FormatCodeTest.class);
    public FormatCodeTest(String name)
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
    
    @Test
    public void testCAMQPTypes() throws Exception
    {
        for (CAMQPTypes type : CAMQPTypes.values())
        {
            int formatCode = type.formatCode();
            String formatName = type.typeName();
            int width = CAMQPCodecUtil.computeWidth(formatCode);
            String output = String.format("FormatName: (%s) FormatCode: (%d) Width: (%d)", formatName, formatCode, width);
            log.debug(output);
        }
    }  
}
