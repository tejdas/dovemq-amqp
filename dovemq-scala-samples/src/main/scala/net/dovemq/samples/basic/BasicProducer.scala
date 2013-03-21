package net.dovemq.samples.basic

import net.dovemq.api.{ ConnectionFactory, MessageFactory }

object BasicProducer {
  def main(args: Array[String]): Unit = {
    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("producer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val producer = session.createProducer(queueName)

    for (i <- 0 until 20) {
      val message = MessageFactory.createMessage()
      val msg = "Hello from Producer: msg: " + i
      println("sending message: " + msg)
      message.addPayload(msg.getBytes())
      producer.sendMessage(message)
    }

    val secondmsg = "Hello from Producer, last message"
    println("sending another message: " + secondmsg)
    producer.sendMessage(secondmsg.getBytes())

    Thread.sleep(10000)
    session.close()
    ConnectionFactory.shutdown()
  }
}