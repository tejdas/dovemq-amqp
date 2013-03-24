package net.dovemq.samples.tag_routing

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

/**
 * This sample shows how to create a DoveMQ subscriber that creates or binds to
 * a topic on the DoveMQ broker, and waits for incoming messages. It also
 * demonstrates that the subscriber is not interested in all the messages from
 * the topic, rather only those with a tag that matches with the message filter
 * pattern.
 */
object TopicSubscriber {
  def main(args: Array[String]): Unit = {

    val topicName = "TagRoutingTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("subscriber")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    /*
     * Create a subscriber that creates/binds to a topic on the broker.
     * Also specify the message filter pattern, so the Topic forwards
     * only those messages that have a tag matching with this pattern.
     */
    val messageFilterPattern = "ab....yz";
    val subscriber = session.createTagFilterSubscriber(topicName, messageFilterPattern);
    System.out.println("Created TopicSubscriber at hierarchy scope: " + topicName);

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