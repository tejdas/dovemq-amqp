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

package net.dovemq.transport.protocol;

public enum Width
{
    FIXED_ZERO (0),
    FIXED_ONE (1),
    FIXED_TWO (2),
    FIXED_FOUR (4),
    FIXED_EIGHT (8),
    FIXED_SIXTEEN (16),    
    VARIABLE_ONE (1),
    VARIABLE_TWO (2),
    VARIABLE_FOUR (4),
    COMPOUND_ONE (1),
    COMPOUND_TWO (2),
    COMPOUND_FOUR (4),    
    ARRAY (4);
    
    private final int widthOctets;

    Width(int widthOctets)
    {
        this.widthOctets = widthOctets;
    }
    
    protected int widthOctets()
    {
        return widthOctets;
    }
}
