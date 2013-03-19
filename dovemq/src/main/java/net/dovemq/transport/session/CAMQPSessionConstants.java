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

public final class CAMQPSessionConstants {
    protected static final long DEFAULT_OUTGOING_WINDOW_SIZE = 16384;

    protected static final long DEFAULT_INCOMING_WINDOW_SIZE = 16384;

    protected static final long MIN_INCOMING_WINDOW_SIZE_THRESHOLD = 8;

    protected static final long BATCHED_DISPOSITION_SEND_INTERVAL = 500L; // milliseconds

    static final long FLOW_SENDER_INTERVAL = 1000L; // milliseconds

    static final int SESSION_SENDER_REQUEST_CREDIT_TIMER_INTERVAL = 1000; // milliseconds

    static final int DEFAULT_SESSION_DISPOSITION_SENDER_THREAD_COUNT = 8;
}
