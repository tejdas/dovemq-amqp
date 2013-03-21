package net.dovemq.samples.basic

import net.dovemq.api.{DoveMQMessageReceiver, DoveMQMessage, ConnectionFactory}

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the Consumer.
 */
class SampleMessageReceiver extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Received message: " + payload)
  }
}

/**
 * This sample shows how to create a DoveMQ consumer that creates a transient
 * queue in the DoveMQ broker, and waits for incoming messages.
 */
object BasicConsumer {

  def main(args: Array[String]): Unit = {

    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("producer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val consumer = session.createConsumer(queueName)

    /*
     * Register a message receiver with the consumer to asynchronously
     * receive messages.
     */
    val messageReceiver = new SampleMessageReceiver()
    consumer.registerMessageReceiver(messageReceiver)

    println("waiting for messages. Press Ctl-C to shut down consumer.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() {
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}