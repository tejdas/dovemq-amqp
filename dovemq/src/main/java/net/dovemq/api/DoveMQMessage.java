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

/**
 * This interface represents an AMQP message
 * and has methods to get/set message properties,
 * header properties, delivery annotations, application
 * properties and message payload.
 *
 * @author tejdas
 *
 */
public interface DoveMQMessage
{
    /**
     * Return message header properties.
     * @return
     */
    public HeaderProperties getHeaderProperties();
    /**
     * Add a delivery annotation {key, value} pair.
     * @param key
     * @param val
     */
    public void addDeliveryAnnotation(String key, String val);
    /**
     * Get a delivery annotation by key.
     * @param key
     * @return
     */
    public String getDeliveryAnnotation(String key);
    /**
     * Get all delivery annotation keys.
     * @return
     */
    public Collection<String> getDeliveryAnnotationKeys();
    /**
     * Add a message annotation {key, value} pair.
     * @param key
     * @param val
     */
    public void addMessageAnnotation(String key, String val);
    /**
     * Get a message annotation by key.
     * @param key
     * @return
     */
    public String getMessageAnnotation(String key);
    /**
     * Get all message annotation keys.
     * @return
     */
    public Collection<String> getMessageAnnotationKeys();
    /**
     * Get MessageProperties.
     * @return
     */
    public MessageProperties getMessageProperties();
    /**
     * Add an application property {key, value} pair.
     * @param key
     * @param val
     */
    public void addApplicationProperty(String key, String val);
    /**
     * Get an application property by key.
     * @param key
     * @return
     */
    public String getApplicationProperty(String key);
    /**
     * Get all application property keys.
     * @return
     */
    public Collection<String> getApplicationPropertyKeys();
    /**
     * Add a payload.
     * The AMQP message can have more than 1 payload.
     * @param payload
     */
    public void addPayload(byte[] payload);
    /**
     * Indicates if the AMQP message has one payload
     * or a collection of more than one payloads.
     * @return
     */
    public boolean hasMultiplePayloads();
    /**
     * Get the message payload,
     * OR
     * If there are multiple payloads, then
     * get the first payload.
     * @return
     */
    public byte[] getPayload();
    /**
     * Get a collection of all payloads.
     * If the message has only one payload,
     * then the collection has a size of 1.
     * @return
     */
    public Collection<byte[]> getPayloads();
    /**
     * Add a footer by {key, value} pair.
     * @param key
     * @param val
     */
    public void addFooter(String key, String val);
    /**
     * Get a footer by key.
     * @param key
     * @return
     */
    public String getFooter(String key);
    /**
     * Get all footer keys.
     * @return
     */
    public Collection<String> getFooterKeys();

    public void setRoutingTag(String tag);
}
