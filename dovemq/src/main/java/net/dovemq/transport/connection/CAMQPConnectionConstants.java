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

package net.dovemq.transport.connection;

public final class CAMQPConnectionConstants {
    public static final int AMQP_IANA_PORT = 5672;

    protected static final int PROTOCOL_ID = 0;

    protected static final int SUPPORTED_MAJOR_VERSION = 1;

    protected static final int SUPPORTED_MINOR_VERSION = 0;

    protected static final int REVISION = 0;

    public static final int HEADER_LENGTH = 8;

    protected static final int MAX_CHANNELS_SUPPORTED = 256;

    protected static final long HEARTBEAT_PERIOD = 30000L; // milliseconds

    protected static final int DEFAULT_HEARTBEAT_PROCESSOR_THREAD_COUNT = 4;

    protected static final long CONNECTION_HANDSHAKE_TIMEOUT = 10000L; // milliseconds
}
