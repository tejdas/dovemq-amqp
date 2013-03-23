package net.dovemq.samples.ack

import net.dovemq.api.{ConnectionFactory, DoveMQMessageReceiver, DoveMQMessage, Consumer, DoveMQEndpointPolicy}
import net.dovemq.api.DoveMQEndpointPolicy._

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the Consumer.
 */
class SampleMessageReceiver(val consumer: Consumer) extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Received message: " + payload)

    /*
     * Explicitly acknowledge the message receipt.
     */
    consumer.acknowledge(message);
  }
}

object BasicAckConsumer {

  def main(args: Array[String]): Unit = {

    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("producer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    /*
     * Create a consumer that binds to a queue on the broker.
     * Also set the MessageAcknowledgementPolicy to CONSUMER_ACKS.
     */
    val endpointPolicy = new DoveMQEndpointPolicy(MessageAcknowledgementPolicy.CONSUMER_ACKS)
    val consumer = session.createConsumer(queueName, endpointPolicy)

    /*
     * Register a message receiver with the consumer to asynchronously
     * receive messages.
     */
    val messageReceiver = new SampleMessageReceiver(consumer)
    consumer.registerMessageReceiver(messageReceiver)

    println("waiting for messages. Press Ctl-C to shut down consumer.")
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