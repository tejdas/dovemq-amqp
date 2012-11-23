DoveMQ
======

A messaging framework built on AMQP 1.0 protocol.

Written in Java.
Uses Netty as the underlying transport framework.

Organization of the code
------------------------

Consists of three maven projects:

1. dovemq : Source for the framework.

  a. The package net.dovemq.transport contains the AMQP protocol implementation:

        protocol
        framing
        connection
        session
        link
        endpoint

  b. The package net.dovemq.broker contains the broker elements:

        Broker driver
        TopicRouter
        QueueRouter

  c. The package net.dovemq.api contains API classes:

        Publisher
        Subscriber
        Producer
        Consumer

2. functional-test : Functional tests for the framework.
3. dovemq-samples : Samples that illustrate how to use the framework.

Build the source
----------------

You need Maven 3.x to build the code.

1. Run:

   mvn clean install -DskipTests

2. Under dovemq, run:

   mvn clean install assembly:assembly -DskipTests

This packages the required jars that's needed to run the broker.

Run the broker
--------------

a. Unzip the tar/zip built in step 2 above.

b. Make sure java is in the path.

c. On Unix, run

  bin/runbroker.sh

  On Windows, run
  
  bin\runbroker.bat



