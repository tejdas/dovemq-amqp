DoveMQ
======

DoveMQ is an [AMQP 1.0](http://www.amqp.org) based messaging framework, written in Java.
It constitutes a standalone AMQP Broker and an API to write messaging applications.

DoveMQ wiki
-----------

[Check out DoveMQ wiki](https://github.com/tejdas/dovemq-amqp/wiki/DoveMQ)

Organization of the code
------------------------

[Code organization](https://github.com/tejdas/dovemq-amqp/blob/master/CODE_ORGANIZATION.md)


Build the source
----------------

You need JDK 1.7 and Maven 3.x to build the code.

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

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright 2012 Tejeswar Das

Licensed under the Apache License, Version 2.0 (the "License");
You may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
