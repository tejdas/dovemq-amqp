package net.dovemq.samples.round_robin

import net.dovemq.api.{ConnectionFactory, Session, DoveMQMessageReceiver, DoveMQMessage, Consumer, DoveMQEndpointPolicy}
import java.util.concurrent.{CountDownLatch, Executors}

/**
 * Implementation of a sample MessageReceiver callback, that is registered
 * with the Consumer.
 */
class SampleMessageReceiver(val id: Integer) extends DoveMQMessageReceiver {
  override def messageReceived(message: DoveMQMessage) = {
    val body = message.getPayload()
    val payload = new String(body)
    println("Consumer id(" + id + ") received message: "  + payload)
  }
}

class SampleConsumer(session: Session, id: Integer, queueName: String, beginSignal: CountDownLatch, doneSignal: CountDownLatch) extends Runnable {
  override def run() {
    try {
      beginSignal.await()
      /*
       * Create a consumer that binds to a transient queue on the
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
      doneSignal.await()
      println("consumer: " + id + " done")
    }
  }
}

object RRConsumer {
  def main(args: Array[String]): Unit = {

    val numConsumers = 4;
    val beginSignal = new CountDownLatch(1);
    val doneSignal = new CountDownLatch(1);
    val executor = Executors.newFixedThreadPool(numConsumers);
    val queueName = "SampleQueue"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("consumer")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    for (i <- 0 until numConsumers) {
      val sampleConsumer = new SampleConsumer(session, i, queueName, beginSignal, doneSignal)
      executor.submit(sampleConsumer)
    }

    beginSignal.countDown()
    println("waiting for messages. Press Ctl-C to shut down consumer.")
    /*
     * Register a shutdown hook to perform graceful shutdown.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() {
        doneSignal.countDown()

        Thread.sleep(2000);
        executor.shutdown();
        session.close()
        ConnectionFactory.shutdown()
      }
    });
  }
}
