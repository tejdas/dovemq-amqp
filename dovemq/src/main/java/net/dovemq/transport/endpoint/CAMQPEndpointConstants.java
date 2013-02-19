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

package net.dovemq.transport.endpoint;

public final class CAMQPEndpointConstants {
    protected static final long MAX_UNSETTLED_MESSAGES_AT_SOURCE = 4096;

    protected static final long UNSETTLED_MESSAGE_THRESHOLD_FOR_SEND_MESSAGE_RESUMPTION = 3072;

    protected static final long MAX_UNSENT_MESSAGES_AT_SOURCE = 10240;

    protected static final long UNSENT_MESSAGE_THRESHOLD_FOR_SEND_MESSAGE_RESUMPTION = 8192;

    protected static final long MAX_WAIT_PERIOD_FOR_UNSENT_MESSAGE_THRESHOLD = 60000; // milliseconds

    protected static final long WAIT_INTERVAL_FOR_UNDELIVERED_MESSAGE_THRESHOLD = 1000; // milliseconds

    protected static final String LINK_SENDER_CONGESTION_EXCEPTION = "AMQP link pipe is congested. Too many unsent messages. Please try sending message later";
}
