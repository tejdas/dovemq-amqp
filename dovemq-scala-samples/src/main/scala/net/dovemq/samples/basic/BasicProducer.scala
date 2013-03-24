package net.dovemq.samples.basic

import net.dovemq.api.{ ConnectionFactory, MessageFactory }

object BasicProducer {
  def main(args: Array[String]): Unit = {
    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    /*
     * Initialize DoveMQ runtime
     */
    ConnectionFactory.initialize("producer")

    /*
     * Create an AMQP session
     */
    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    /*
     * Create a producer that binds to a queue on the broker.
     */
    val producer = session.createProducer(queueName)

    /*
     * Create and send some messages.
     */
    for (i <- 0 until 20) {
      val message = MessageFactory.createMessage()
      val msg = "Hello from Producer: msg: " + i
      println("sending message: " + msg)
      message.addPayload(msg.getBytes())
      producer.sendMessage(message)
    }

    /*
     * Another way of sending message.
     */
    val secondmsg = "Hello from Producer, last message"
    println("sending another message: " + secondmsg)
    producer.sendMessage(secondmsg.getBytes())

    Thread.sleep(10000)
    /*
     * Close AMQP session
     */
    session.close()
    /*
     * Cleanup DoveMQ runtime
     */
    ConnectionFactory.shutdown()
  }
}