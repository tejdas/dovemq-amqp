package net.dovemq.samples.pubsub

import net.dovemq.api.{ ConnectionFactory, MessageFactory }

/**
 * This sample shows how to create a DoveMQ publisher that creates/binds to a
 * Topic on the DoveMQ broker, and sends messages.
 */
object TopicPublisher {

  def main(args: Array[String]): Unit = {
    val topicName = "SampleTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("publisher")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    /*
     * Create a publisher that creates/binds to a topic on the broker.
     */
    val publisher = session.createPublisher(topicName)

    /*
     * Create and publish some messages.
     */
    for (i <- 0 until 20) {
      val message = MessageFactory.createMessage()
      val msg = "Hello from Producer: msg: " + i
      println("sending message: " + msg)
      message.addPayload(msg.getBytes())
      publisher.publishMessage(message)
    }

    /*
     * Another way of publishing messages.
     */
    val secondmsg = "Hello from Producer, last message"
    println("sending another message: " + secondmsg)
    publisher.publishMessage(secondmsg.getBytes())

    Thread.sleep(10000)
    session.close()
    ConnectionFactory.shutdown()
  }
}