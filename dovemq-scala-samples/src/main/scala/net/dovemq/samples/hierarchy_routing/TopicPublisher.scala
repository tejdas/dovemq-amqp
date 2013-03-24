package net.dovemq.samples.hierarchy_routing

import net.dovemq.api.{ConnectionFactory, MessageFactory, DoveMQMessage}

/**
 * This sample shows how to create a DoveMQ publisher that creates/binds to a
 * Topic on the DoveMQ broker, and publishes messages. It also demonstrates how
 * the message is targeted to a publish hierarchy scope.
 */
object TopicPublisher {

  def main(args: Array[String]): Unit = {
    val brokerIp = System.getProperty("dovemq.broker", "localhost")

    ConnectionFactory.initialize("publisher")
    val session = ConnectionFactory.createSession(brokerIp)
    println("created session to DoveMQ broker running at: " + brokerIp)

    {
      /*
       * published above subscriber's hierarchy, so it should not receive it.
       */
      val topicHierarchy = "sports";
      val publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
      val message = createMessage("First message: Published at hierarchy: " + topicHierarchy);
      publisher.publishMessage(message);
    }

    {
      /*
       * published at subscriber's hierarchy, so it should receive it.
       */
      val topicHierarchy = "sports.cricket";
      val publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
      val message = createMessage("Second message: Published at hierarchy: " + topicHierarchy);
      publisher.publishMessage(message);
    }

    {
      /*
       * published under subscriber's hierarchy, so it should receive it.
       */
      val topicHierarchy = "sports.cricket.test";
      val publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
      val message = createMessage("Third message: Published at hierarchy: " + topicHierarchy);
      publisher.publishMessage(message);
    }

    {
      /*
       * Not published at/under subscriber's hierarchy, so it should not receive it.
       */
      val topicHierarchy = "sports.cricketers";
      val publisher = session.createHierarchicalTopicPublisher(topicHierarchy);
      val message = createMessage("Fourth message: Published at hierarchy: " + topicHierarchy);
      publisher.publishMessage(message);
    }

    {
      /*
       * The following example shows how the Publisher's topic hierarchy scope could be overridden by
       * a message-specific hierarchy scope.
       *
       * Publisher not bound at/under subscriber's hierarchy, but we are setting
       * a hierarchy scope directly on the message, that is under subscriber's hierarchy,
       * so it should receive it.
       */
      val topicHierarchy = "sports"
      val publisher = session.createHierarchicalTopicPublisher(topicHierarchy)
      val messageLevelHierarchy = "sports.cricket.odi"
      val message = createMessage("Fifth message: Published at hierarchy: " + messageLevelHierarchy)
      message.setTopicPublishHierarchy(messageLevelHierarchy)
      publisher.publishMessage(message)
    }

    session.close()
    ConnectionFactory.shutdown()
  }

  private def createMessage(messageBody: String): DoveMQMessage = {
    val message = MessageFactory.createMessage()
    println("publishing message: " + messageBody)
    message.addPayload(messageBody.getBytes())
    message
  }
}