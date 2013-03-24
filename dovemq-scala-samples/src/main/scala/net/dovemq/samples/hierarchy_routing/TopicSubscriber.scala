package net.dovemq.samples.hierarchy_routing

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
 * This sample shows how to create a DoveMQ Hierarchical subscriber that creates
 * or binds to a certain hierarchy under the root topic and waits for incoming
 * messages. It only gets messages that are published at or below that
 * hierarchy.
 */
object TopicSubscriber {
  def main(args: Array[String]): Unit = {

    val topicName = "sports.cricket"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("subscriber")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    /*
     * Create a subscriber that creates/binds to a hierarchical-scoped
     * topic on the broker.Only those messages that
     * are published at/below this hierarchy are routed to the
     * subscriber.
     *
     * e.g.
     * sports.cricket.test
     * sports.cricket.india
     *
     * but not
     *
     * sports.baseball
     * sports
     */
    val subscriber = session.createHierarchicalTopicSubscriber(topicName);
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