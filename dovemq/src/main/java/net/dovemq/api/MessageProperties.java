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

import java.util.Date;

public interface MessageProperties
{
    public String getMessageId();
    public void setMessageId(String messageId);
    public String getCorrlelationId();
    public void setCorrlelationId(String corrlelationId);
    public String getUserId();
    public void setUserId(String userId);
    public String getToAddress();
    public void setToAddress(String toAddress);
    public String getReplyToAddress();
    public void setReplyToAddress(String replyToAddress);
    public String getSubject();
    public void setSubject(String subject);
    public String getContentType();
    public void setContentType(String contentType);
    public Date getCreationTime();
    public void setCreationTime(Date creationTime);
    public Date getExpiryTime();
    public void setExpiryTime(Date expiryTime);
}
