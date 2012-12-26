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

import net.jcip.annotations.Immutable;

@Immutable
public final class CAMQPCompundHeader {
    public final int elementFormatCode;

    public final long elementCount;

    public CAMQPCompundHeader(int elementFormatCode, long elementCount) {
        super();
        this.elementFormatCode = elementFormatCode;
        this.elementCount = elementCount;
    }
}
