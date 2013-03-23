package net.dovemq.samples.pubsub

import net.dovemq.api.{DoveMQMessageReceiver, DoveMQMessage, ConnectionFactory}

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the Subscriber.
 */
class SampleMessageReceiver extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Received message: " + payload)
  }
}

object TopicSubscriber {
 def main(args: Array[String]): Unit = {

    val topicName = "SampleTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("producer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val subscriber = session.createSubscriber(topicName)

    /*
     * Register a message receiver with the subscriber to asynchronously
     * receive messages.
     */
    val messageReceiver = new SampleMessageReceiver()
    subscriber.registerMessageReceiver(messageReceiver)

    println("waiting for messages. Press Ctl-C to shut down subscriber.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() = {
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}