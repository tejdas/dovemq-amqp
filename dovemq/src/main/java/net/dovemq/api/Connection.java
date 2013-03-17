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

import net.dovemq.transport.connection.CAMQPConnectionInterface;
import net.dovemq.transport.session.CAMQPSessionFactory;
import net.dovemq.transport.session.CAMQPSessionInterface;

/**
 * This class encapsulates an AMQP connection. It also acts as a factory
 * to create Session objects.
 *
 * @author tdas
 *
 */
public final class Connection {

    /**
     * Creates a new AMQP session to the target DoveMQ broker over this
     * AMQP connection
     *
     * @return
     *      Session, that encapsulates an AMQP session
     */
    public Session createSession() {
        CAMQPSessionInterface camqpSession = CAMQPSessionFactory.createCAMQPSession(amqpConnection);
        return new Session(ConnectionFactory.getEndpointId(), camqpSession);
    }

    /**
     * Closes the underlying AMQP connection
     */
    public void close() {
        amqpConnection.close();
    }

    Connection(CAMQPConnectionInterface amqpConnection) {
        super();
        this.amqpConnection = amqpConnection;
    }

    /**
     * AMQP connection
     */
    private final CAMQPConnectionInterface amqpConnection;
}
