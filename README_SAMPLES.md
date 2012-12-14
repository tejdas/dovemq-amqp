How to build the samples
========================

You need JDK 1.6+ and Maven 3.x to build the code.

Under dovemq-samples directory, run:

    mvn clean install

This creates the following uber jar containing all the dependent jars, under the target directory:

    dovemq-samples-1.0-SNAPSHOT.jar

How to run DoveMQ samples
=========================

a. Run DoveMQ broker.

b. Run a sample class as following:


    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=sample.log -Ddovemq.broker={BROKER_IP} {SAMPLE_MAIN_CLASS}

Example:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=consumer.log -Ddovemq.broker=localhost net.dovemq.samples.basic.BasicConsumer

Description of Samples
======================

**basic**
---------

**BasicConsumer**: Creates a DoveMQ consumer that binds to a transient queue and waits for message.

**BasicProducer**: Creates a DoveMQ producer that binds to a transient queue and sends messages.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=consumer.log -Ddovemq.broker=localhost net.dovemq.samples.basic.BasicConsumer

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=producer.log -Ddovemq.broker=localhost net.dovemq.samples.basic.BasicProducer

**round_robin**
---------------

**RRConsumer**: Creates a set of DoveMQ consumers that connect to a transient queue and wait for message. When a producer
sends messages, it is dispatched to the attached consumers in a round-robin fashion.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=consumer.log -Ddovemq.broker=localhost net.dovemq.samples.round_robin.RRConsumer

**ack**
-------

**BasicAckConsumer**: Creates a DoveMQ consumer that creates a transient queue in the DoveMQ broker,
and waits for incoming messages. The consumer is created with a CONSUMER_ACKS mode, which means
the receiver needs to explicitly acknowledge receipt of the message.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=producer.log -Ddovemq.broker=localhost net.dovemq.samples.ack.BasicAckConsumer

Run **BasicProducer** to send message.

**pubsub**
----------

**TopicSubscriber**: Creates a DoveMQ subscriber that creates or binds to a topic on the DoveMQ broker, and waits for incoming messages.

**TopicPublisher**: Creates a DoveMQ publisher that creates or binds to a topic on the DoveMQ broker, and publishes messages.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=subscriber.log -Ddovemq.broker=localhost net.dovemq.samples.pubsub.TopicSubscriber

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=publisher.log -Ddovemq.broker=localhost net.dovemq.samples.pubsub.TopicPublisher

**TopicMultipleSubscribers**: Creates a set of DoveMQ subscribers that connect to a topic on the DoveMQ broker, and wait for incoming messages. Incoming messages to the topic are multicasted to all the attached subscribers.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=subscriber.log -Ddovemq.broker=localhost net.dovemq.samples.pubsub.TopicMultipleSubscribers

**rpc**
-------

This sample demonstrates how two DoveMQ endpoints use a pair of request/reply queues to communicate in an rpc-style. The Requestor sends a request message to a request queue and waits for response message at the response queue. The Responder receives the request message
and send a response back to response queue, putting the message id of the incoming message as the correlation id of the outgoing message. The Requestor uses the correlation ID to correlate the response with the corresponding request.

To run:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=responder.log -Ddovemq.broker=localhost net.dovemq.samples.rpc.Responder
    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=requester.log -Ddovemq.broker=localhost net.dovemq.samples.rpc.Requester
