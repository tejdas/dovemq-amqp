package net.dovemq.samples.rpc;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

/**
 * This sample demonstrates how to simulate RPC style communication by using a
 * pair of queues, one for sending request and another for receiving response.
 * The outgoing message is tagged with a messageId. The Responder sends a
 * response message that is tagged with a correlationId that is the same as the
 * messageId. The requester uses the correlationId to match the incoming
 * response for the outgoing request.
 */
public class Requester {
    /*
     * Outgoing request messages are stored until a response message has been
     * received.
     */
    private static final ConcurrentMap<String, DoveMQMessage> outstandingRequests = new ConcurrentHashMap<String, DoveMQMessage>();

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        /**
         * Receive the response message. Match its correlationId with the
         * request message's messageId and remove the request message from
         * outstandingRequests map.
         */
        @Override
        public void messageReceived(DoveMQMessage message) {
            String correlationId = message.getMessageProperties()
                    .getCorrlelationId();
            DoveMQMessage requestMessage = outstandingRequests.remove(correlationId);
            if (requestMessage != null) {
                System.out.println("received response for requestId: " + correlationId);

                byte[] body = message.getPayload();
                String payload = new String(body);
                System.out.println("Response payload: " + payload);
            }
        }
    }

    /*
     * The queue used to send the request message to.
     */
    private static final String TO_ADDRESS = "requestQueue";

    /*
     * The queue used to receive the response message from.
     */
    private static final String REPLY_TO_ADDRESS = "responseQueue";

    public static void main(String[] args) throws InterruptedException {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("rpcRequester");

        try {
            /*
             * Create an AMQP session.
             */
            Session session = ConnectionFactory.createSession(brokerIp);
            System.out.println("created session to DoveMQ broker running at: " + brokerIp);

            /*
             * Create a producer to send the request message to.
             */
            Producer producer = session.createProducer(TO_ADDRESS);

            /*
             * Create a consumer and register a message receiver to receive the
             * response message from.
             */
            Consumer consumer = session.createConsumer(REPLY_TO_ADDRESS);
            consumer.registerMessageReceiver(new SampleMessageReceiver());

            /*
             * Create and send a request message.
             */
            DoveMQMessage message = MessageFactory.createMessage();
            String messageId = UUID.randomUUID().toString();
            message.getMessageProperties().setMessageId(messageId);
            message.getMessageProperties().setReplyToAddress(REPLY_TO_ADDRESS);

            /*
             * Put the message in the outstandingRequests map. It is removed
             * when a response is received.
             */
            outstandingRequests.put(messageId, message);

            String msg = "Request from Producer";
            System.out.println("sending message: " + msg);
            message.addPayload(msg.getBytes());
            producer.sendMessage(message);

            System.out.println("waiting for response");
            Thread.sleep(10000);

            /*
             * Close the AMQP session
             */
            session.close();
        }
        finally {
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
