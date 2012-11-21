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

package net.dovemq.api;

import java.util.Collection;

public interface DoveMQMessage
{
    public HeaderProperties getHeaderProperties();
    public void addDeliveryAnnotation(String key, String val);
    public String getDeliveryAnnotation(String key);
    public Collection<String> getDeliveryAnnotationKeys();
    public void addMessageAnnotation(String key, String val);
    public String getMessageAnnotation(String key);
    public Collection<String> getMessageAnnotationKeys();
    public MessageProperties getMessageProperties();
    public void addApplicationProperty(String key, String val);
    public String getApplicationProperty(String key);
    public Collection<String> getApplicationPropertyKeys();
    public void addPayload(byte[] payload);
    public boolean hasMultiplePayloads();
    public byte[] getPayload();
    public Collection<byte[]> getPayloads();
    public void addFooter(String key, String val);
    public String getFooter(String key);
    public Collection<String> getFooterKeys();
}
