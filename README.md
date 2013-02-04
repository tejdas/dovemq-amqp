DoveMQ
======

AMQP 1.0 protocol based messaging framework.

Organization of the code
------------------------

[Code organization](https://github.com/tejdas/dovemq-amqp/blob/master/CODE_ORGANIZATION.md)


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

DoveMQ samples
--------------

[Check out DoveMQ samples for specific examples on how to build and run the samples](https://github.com/tejdas/dovemq-amqp/blob/master/README_SAMPLES.md)
