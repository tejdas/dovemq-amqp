package net.dovemq.samples.pubsub

import net.dovemq.api.{ConnectionFactory, Session, DoveMQMessageReceiver, DoveMQMessage, DoveMQEndpointPolicy}
import scala.collection.mutable.ListBuffer
import scala.actors.Actor

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the subscriber.
 */
private class SampleTopicReceiver(val id: Integer) extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Subscriber id(" + id + ") received message: "  + payload)
  }
}

abstract class Command
case class Initialize extends Command
case class Shutdown extends Command

/**
 * Actor that represents a subscriber
 */
class SubscriberActor(session: Session, id: Integer, topicName: String) extends Actor {
  def act() = {
    while (true) {
      receive {
        case Initialize =>
          /*
           * Create a subscriber that binds to a topic on the
           * broker.
           */
          val subscriber = session.createSubscriber(topicName)

          /*
           * Register a message receiver with the subscriber to
           * asynchronously receive messages.
           */
          val messageReceiver = new SampleTopicReceiver(id)
          subscriber.registerMessageReceiver(messageReceiver)

          println("created subscriber: id(" + id + ") , waiting for messages")

        case Shutdown =>
          println("subscriber: " + id + " done")
          exit
      }
    }
  }
}

/**
 * This sample shows how to create a DoveMQ subscriber that creates a Topic in
 * the DoveMQ broker, and waits for incoming messages. It creates multiple
 * subscribers attached to the Topic. When Publisher publishes messages on the
 * topic, it is multi-casted to all Subscribers.
 */
object TopicMultipleSubscribers {
 def main(args: Array[String]): Unit = {

    val numSubscribers = 4;
    val topicName = "SampleTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("subscriber")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    var subscribers = ListBuffer[SubscriberActor]()

    for (i <- 0 until numSubscribers) {
      subscribers += new SubscriberActor(session, i, topicName)
    }

    subscribers.foreach(subscriber => subscriber.start)
    subscribers.foreach(subscriber => subscriber !  Initialize)

    println("waiting for messages. Press Ctl-C to shut down subscriber.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() = {
        subscribers.foreach(subscriber => subscriber !  Shutdown)

        Thread.sleep(2000);
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}