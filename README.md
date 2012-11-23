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

Under dovemq directory, run:

   mvn clean assembly:assembly -DskipTests

This creates a tar/zip with all the required jars that's needed to run the broker:

dovemq-1.0-SNAPSHOT-bin.tar.gz

dovemq-1.0-SNAPSHOT-bin.zip

Run the broker
--------------

a. Unzip the tar or zip that was generated in the above step.

b. Make sure java is in the path.

c. On Unix, run

  bin/runbroker.sh

  On Windows, run
  
  bin\runbroker.bat

Build the samples
-----------------

You need Maven 3.x to build the code.

Under dovemq-samples directory, run:

   mvn clean assembly:assembly -DskipTests

This creates a tar/zip with all the required jars that's needed to run the samples:

dovemq-samples-1.0-SNAPSHOT-bin.tar.gz

dovemq-samples-1.0-SNAPSHOT-bin.zip

Run the samples
---------------

a. Unzip the tar or zip that was generated in the above step.

b. Make sure java is in the path.

c. Run a sample class:

java -cp dovemq-samples-1.0-SNAPSHOT.jar:dovemq-1.0-SNAPSHOT.jar:log4j-1.2.15.jar:netty-3.2.4.Final.jar:commons-lang-2.5.jar -Ddovemq.log=sample.log -Ddovemq.broker=<BROKER_IP> <SAMPLE_MAIN_CLASS>

See the README.txt in the samples for specific examples on how to run the samples.
