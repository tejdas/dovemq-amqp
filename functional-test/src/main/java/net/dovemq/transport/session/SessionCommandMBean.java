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

import java.util.Collection;

public interface SessionCommandMBean
{
    public void registerFactory(String factoryName);
    public void sessionCreate(String targetContainerId);
    public void sessionCreateMT(String targetContainerId, int numThreads);
    public Collection<Integer> getChannelId(String targetContainerId);
    public void sessionIO(String targetContainerId, String source, String dest);
    public void sessionClose(String targetContainerId, int channelId);
    public void setSessionWindowSize(long maxOutgoingWindowSize, long maxIncomingWindowSize);
    public boolean isIODone();
}
