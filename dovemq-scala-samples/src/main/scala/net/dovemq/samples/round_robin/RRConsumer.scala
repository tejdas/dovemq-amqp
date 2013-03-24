package net.dovemq.samples.round_robin

import net.dovemq.api.{ConnectionFactory, Session, DoveMQMessageReceiver, DoveMQMessage, Consumer, DoveMQEndpointPolicy}
import scala.actors.Actor
import scala.collection.mutable.ListBuffer

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the Consumer.
 */
private class SampleMessageReceiver(val id: Integer) extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Consumer id(" + id + ") received message: "  + payload)
  }
}

abstract class Command
case class Initialize extends Command
case class Shutdown extends Command

/**
 * Actor that represents a consumer
 */
class ConsumerActor(session: Session, id: Integer, queueName: String) extends Actor {
  def act() = {
    while (true) {
      receive {
        case Initialize =>
          /*
           * Create a consumer that binds to a queue on the
           * broker.
           */
          val consumer = session.createConsumer(queueName)

          /*
           * Register a message receiver with the consumer to
           * asynchronously receive messages.
           */
          val messageReceiver = new SampleMessageReceiver(id)
          consumer.registerMessageReceiver(messageReceiver)

          println("created consumer: id(" + id + ") , waiting for messages")

        case Shutdown =>
          println("consumer: " + id + " done")
          exit
      }
    }
  }
}

/**
 * This sample shows how to create a DoveMQ consumer that creates a
 * queue in the DoveMQ broker, and waits for incoming messages. It creates
 * multiple consumers attached to the queue. When Producer sends messages to the
 * queue, it is routed to all the consumers in a round-robin fashion.
 */
object RRConsumer {
  def main(args: Array[String]): Unit = {

    val numConsumers = 4;
    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("consumer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val consumers = ListBuffer[ConsumerActor]()

    for (i <- 0 until numConsumers) {
      consumers += new ConsumerActor(session, i, queueName)
    }

    consumers.foreach(consumer => consumer.start)
    consumers.foreach(consumer => consumer !  Initialize)

    println("waiting for messages. Press Ctl-C to shut down consumer.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() = {
        consumers.foreach(consumer => consumer !  Shutdown)

        Thread.sleep(2000);
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}
