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

package net.dovemq.transport.frame;

import net.jcip.annotations.Immutable;

import org.jboss.netty.buffer.ChannelBuffer;


@Immutable
public final class CAMQPHandshakeFrame {
    public CAMQPHandshakeFrame(ChannelBuffer body) {
        super();
        this.body = body;
    }

    public ChannelBuffer getBody() {
        return body;
    }

    private final ChannelBuffer body;
}
