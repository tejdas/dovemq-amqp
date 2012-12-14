DoveMQ
======

AMQP 1.0 protocol based messaging framework.

Organization of the code
------------------------

Consists of three maven projects:

1. **dovemq** : Source for the framework.

  a. The package *net.dovemq.transport* contains the AMQP protocol implementation:

        protocol
        framing
        connection
        session
        link
        endpoint

  b. The package *net.dovemq.broker* contains the broker elements:

        Broker driver
        TopicRouter
        QueueRouter

  c. The package *net.dovemq.api* contains API classes:

        Connection
        Session
        Publisher
        Subscriber
        Producer
        Consumer
        DoveMQMessage
        DoveMQMessageReceiver
        DoveMQMessageAckReceiver

2. **functional-test** : Functional tests for the framework.

  You need ant to run the functional tests.
  (see *src/main/resources/build.xml*)

3. **dovemq-samples** : Samples that illustrate how to use the framework.

Build the source
----------------

You need JDK 1.6+ and Maven 3.x to build the code.

Under dovemq directory, run:

    mvn clean assembly:assembly -DskipTests

This creates a tar/zip with all the required jars that's needed to run the broker:

    dovemq-1.0-SNAPSHOT-bin.tar.gz
    dovemq-1.0-SNAPSHOT-bin.zip

Run the broker
--------------

a. Unzip the tar or zip that was generated in the above step.

b. On Unix, run

    bin/runbroker.sh

  On Windows, run
  
    bin\runbroker.bat

Build the samples
-----------------

You need Maven 3.x to build the code.

Under dovemq-samples directory, run:

    mvn clean install

This creates the following uber jar containing all the dependent jars, under the target directory:

    dovemq-samples-1.0-SNAPSHOT.jar

Run the samples
---------------

a. Make sure java is in the path.

b. Run a sample class:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=sample.log -Ddovemq.broker={BROKER_IP} {SAMPLE_MAIN_CLASS}

Example:

    java -cp dovemq-samples-1.0-SNAPSHOT.jar -Ddovemq.log=consumer.log -Ddovemq.broker=localhost net.dovemq.samples.basic.BasicConsumer

See the README_SAMPLES.md for specific examples on how to run the samples.
