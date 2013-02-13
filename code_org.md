DoveMQ: Organization of the code
--------------------------------

Consists of three maven projects:

***dovemq***
------------

Source for the framework.

  a. The package *net.dovemq.transport* contains the AMQP protocol implementation:

        protocol
        framing
        connection
        session
        link
        endpoint

  b. The package *net.dovemq.api* contains API classes:

        Connection
        Session
        Publisher
        Subscriber
        Producer
        Consumer
        DoveMQMessage
        DoveMQMessageReceiver
        DoveMQMessageAckReceiver

***functional-test***
---------------------

Functional tests for the framework.

  You need ant to run the functional tests.
  (see *src/main/resources/build.xml*)

***dovemq-samples***
--------------------

Samples that illustrate how to use the framework.

