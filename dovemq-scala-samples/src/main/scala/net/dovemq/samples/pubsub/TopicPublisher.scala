package net.dovemq.samples.pubsub

import net.dovemq.api.{ ConnectionFactory, MessageFactory }

object TopicPublisher {

  def main(args: Array[String]): Unit = {}
    val topicName = "SampleTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("publisher")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val publisher = session.createPublisher(topicName)

    for (i <- 0 until 20) {
      val message = MessageFactory.createMessage()
      val msg = "Hello from Producer: msg: " + i
      println("sending message: " + msg)
      message.addPayload(msg.getBytes())
      publisher.publishMessage(message)
    }

    val secondmsg = "Hello from Producer, last message"
    println("sending another message: " + secondmsg)
    publisher.publishMessage(secondmsg.getBytes())

    Thread.sleep(10000)
    session.close()
    ConnectionFactory.shutdown()
}