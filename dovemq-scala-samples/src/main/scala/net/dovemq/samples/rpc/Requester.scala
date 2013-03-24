package net.dovemq.samples.rpc

import net.dovemq.api.{DoveMQMessageReceiver, DoveMQMessage, ConnectionFactory, MessageFactory}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * This sample demonstrates how to simulate RPC style communication by using a
 * pair of queues, one for sending request and another for receiving response.
 * The outgoing message is tagged with a messageId. The Responder sends a
 * response message that is tagged with a correlationId that is the same as the
 * messageId. The requester uses the correlationId to match the incoming
 * response for the outgoing request.
 */
object Requester {
  /**
   * Synchronization point for message response.
   */
  sealed class MessageSynchronizer() {
    private var replyMessage: DoveMQMessage = null
    def update(replyMsg: DoveMQMessage) = synchronized {
      replyMessage = replyMsg;
      notify();
    }

    def getResponse(timeout: Long) : DoveMQMessage = synchronized ({
      var now = System.currentTimeMillis();
      val expiryTime = now + timeout
      while ((replyMessage == null) && (now < expiryTime)) {
        wait(expiryTime - now)
        now = System.currentTimeMillis()
      }
      replyMessage
    })
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

    /*
     * Create a producer to send the request message to.
     */
    val producer = session.createProducer(toAddress)

    /*
     * Create a consumer and register a message receiver to receive the
     * response message from.
     */
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
    val response = messageSynchronizer.getResponse(5000)

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