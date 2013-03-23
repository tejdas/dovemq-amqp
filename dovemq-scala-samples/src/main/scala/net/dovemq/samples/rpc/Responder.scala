package net.dovemq.samples.rpc

import net.dovemq.api.{DoveMQMessageReceiver, Session, DoveMQMessage, ConnectionFactory, MessageFactory, Producer}
import scala.actors.Actor
import scala.collection.mutable.Map

abstract class Command

case class Shutdown extends Command
case class CreateProducer() extends Command
case class ReceiveRequest(val messageId: String) extends Command

private class ResponseSender(replyToAddress : String, session : Session) extends Actor {
  var producer: Producer = null
  override def act() = {
    while (true) {
      receive {
        case CreateProducer =>
          if (producer == null) {
            producer = session.createProducer(replyToAddress)
          } else {
            println ("shouldn't have received CreateProducer event")
            exit
          }
        case ReceiveRequest(messageId) =>
          if (producer != null) {
            println("ResponseSender received message")
            val response = MessageFactory.createMessage()
            response.getMessageProperties().setCorrlelationId(messageId)
            response.addPayload("Reply from consumer".getBytes())
            producer.sendMessage(response)
          } else {
            println("Hasn't received CreateProducer event yet")
            exit
          }

        case Shutdown =>
          println("ResponseSender done: " + replyToAddress)
          exit
      }
    }
  }
}
case class MessageWrapper(val request: DoveMQMessage) extends Command

private class RequestProcessor(session: Session) extends Actor {
  private val responseSenderMap = Map.empty[String, ResponseSender]

  override def act() = {
    while (true) {
      receive {
        case MessageWrapper(request) =>
          val body = request.getPayload()
          val payload = if (body != null) new String(body) else null
          val replyToAddress = request.getMessageProperties().getReplyToAddress()
          val messageId = request.getMessageProperties().getMessageId()
          var responseSender = if (responseSenderMap.contains(replyToAddress)) responseSenderMap(replyToAddress) else null
          if (responseSender == null) {
            responseSender = new ResponseSender(replyToAddress, session)
            responseSenderMap += (replyToAddress -> responseSender)
            responseSender.start()
            responseSender ! CreateProducer
          }
          responseSender ! ReceiveRequest(messageId)

        case Shutdown =>
          responseSenderMap.foreach {
            case (_, responseSender) => responseSender ! Shutdown
          }
          println("RequestProcessor done")
          exit
      }
    }
  }
}

private class SampleMessageReceiver(val requestProcessor : RequestProcessor) extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    requestProcessor ! MessageWrapper(message)
  }
}

object Responder {
  def main(args: Array[String]): Unit = {

    val listenToAddress = "requestQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("rpcResponder")
    val session = ConnectionFactory.createSession(brokerIp)
    val requestProcessor = new RequestProcessor(session)
    requestProcessor.start()
    val consumer = session.createConsumer(listenToAddress);
    consumer.registerMessageReceiver(new SampleMessageReceiver(requestProcessor));

    println("waiting for messages. Press Ctl-C to shut down consumer.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() = {
        requestProcessor !  Shutdown

        Thread.sleep(2000);
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}