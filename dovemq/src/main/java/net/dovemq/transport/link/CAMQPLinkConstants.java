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

package net.dovemq.transport.link;

public final class CAMQPLinkConstants {
    static final boolean ROLE_SENDER = false;

    static final boolean ROLE_RECEIVER = true;

    public static final long DEFAULT_MAX_MESSAGE_SIZE = 32768;

    public static final long DEFAULT_MAX_AVAILABLE_MESSAGES_AT_SENDER = 1024 * 1024;

    static final long LINK_CREDIT_VIOLATION_LIMIT = 10L;

    static final int MAX_LINK_CREDIT_ISSUANCE_INTERVAL = 1000; // milliseconds

    protected static final long LINK_HANDSHAKE_TIMEOUT = 10000L; // milliseconds

    protected static final long LINK_WAIT_TIME_FOR_CREDIT = 30000L; // milliseconds
}
