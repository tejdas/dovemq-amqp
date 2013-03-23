package net.dovemq.samples.rpc

import net.dovemq.api.{DoveMQMessageReceiver, DoveMQMessage, ConnectionFactory, MessageFactory}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Requester {
  sealed class MessageSynchronizer() {
    private var replyMessage: DoveMQMessage = null
    def update(replyMsg: DoveMQMessage) = synchronized {
      replyMessage = replyMsg;
      notify();
    }

    def getUpdatedValue(timeout: Long) : DoveMQMessage = synchronized {
      while (replyMessage == null) {
        wait(timeout)
        if (replyMessage == null) {
          return null
        }
      }
      replyMessage
    }
  }

  val outstandingRequests = new ConcurrentHashMap[String, MessageSynchronizer]()

  class SampleMessageReceiver extends DoveMQMessageReceiver {
    override def messageReceived(message: DoveMQMessage) = {
      val correlationId = message.getMessageProperties().getCorrlelationId()
      val requestMessageSynchronizer = outstandingRequests.remove(correlationId)
      if (requestMessageSynchronizer != null) {
        println("received response for requestId: " + correlationId)
        requestMessageSynchronizer.update(message)
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val toAddress = "requestQueue"
    val replyToAddress = "responseQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("rpcRequester")

    val session = ConnectionFactory.createSession(brokerIp)

    val producer = session.createProducer(toAddress)

    val consumer = session.createConsumer(replyToAddress)
    consumer.registerMessageReceiver(new SampleMessageReceiver())

    val request = MessageFactory.createMessage()
    val messageId = UUID.randomUUID().toString()
    request.getMessageProperties().setMessageId(messageId)
    request.getMessageProperties().setReplyToAddress(replyToAddress)
    val msg = "Request from Producer";
    println("sending message: " + msg);
    request.addPayload(msg.getBytes())

    val messageSynchronizer = new MessageSynchronizer()
    outstandingRequests.put(messageId, messageSynchronizer)
    producer.sendMessage(request)
    val response = messageSynchronizer.getUpdatedValue(5000)

    if (response != null) {
      val body = response.getPayload()
      val payload = new String(body)
      println("Response payload: " + payload)
    } else {
      println("Timed out waiting for response")
    }

    ConnectionFactory.shutdown()
  }
}