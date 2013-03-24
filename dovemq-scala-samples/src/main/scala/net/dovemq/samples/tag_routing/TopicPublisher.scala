package net.dovemq.samples.tag_routing

import net.dovemq.api.{ ConnectionFactory, MessageFactory }

/**
 * This sample shows how to create a DoveMQ publisher that creates/binds to a
 * Topic on the DoveMQ broker, and publishes messages. It also demonstrates how
 * the message is sent with a routing tag.
 */
object TopicPublisher {
  def main(args: Array[String]): Unit = {
    val topicName = "TagRoutingTopic"
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("publisher")

    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    val publisher = session.createTagFilterPublisher(topicName)

    /*
     * Create and publish a message, with a matching routing tag, so
     * subscriber should see it.
     */
    val message = MessageFactory.createMessage();
    val msg = "Hello from Publisher, the subscriber should get this message";
    println("publishing message: " + msg);
    message.addPayload(msg.getBytes());
    message.setRoutingTag("abcdefyz");
    publisher.publishMessage(message);

    /*
     * Publish another message with no routing tag, so it should not be
     * sent to subscriber.
     */
    val secondmsg = "Hello again from Publisher, second message: the subscriber should not get this message";
    println("sending another message: " + secondmsg);
    publisher.publishMessage(secondmsg.getBytes());

    /*
     * Create and publish a message with a routing tag that does not
     * match
     */
    val message3 = MessageFactory.createMessage();
    val msg3 = "Hello from Publisher, third message: the subscriber should not get this message";
    println("publishing message: " + msg3);
    message3.addPayload(msg3.getBytes());
    message3.setRoutingTag("abcmnopdefyz");
    publisher.publishMessage(message3);

    Thread.sleep(2000)
    session.close()
    ConnectionFactory.shutdown()
  }
}