package net.dovemq.samples.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.Consumer;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.Producer;
import net.dovemq.api.Session;

/**
 * This sample demonstrates how to simulate RPC style communication
 * by using a pair of queues, one for sending request and another
 * for receiving response. The incoming request message is tagged with a
 * messageId. The Responder sends a response message that is tagged
 * with a correlationId that is the same as the messageId. The requester
 * uses the correlationId to match the incoming response for the outgoing
 * request.
 */
public class Responder
{
    private static final String LISTEN_TO_ADDRESS = "requestQueue";
    private static volatile boolean doShutdown = false;

    /*
     * A map of Producer keyed by the replyTo queue name.
     */
    private static final Map<String, Producer> messageProducers = new HashMap<String, Producer>();

    /*
     * Incoming message queue used by the MessageReceiver to hand-off to a RequestProcessor
     */
    private static final BlockingQueue<DoveMQMessage> incomingRequests = new LinkedBlockingQueue<DoveMQMessage>();

    /**
     * This class processes the incoming requests. Gets the replyTo property and
     * gets a corresponding Producer from the messageProducers map (creates one if
     * necessary). It then creates a response with a correlationId that is same as
     * the messageId of the incoming message, and sends the response back on the Producer.
     *
     */
    private static final class RequestProcessor implements Runnable
    {
        public RequestProcessor(Session session)
        {
            super();
            this.session = session;
        }

        @Override
        public void run()
        {
            while (!shutdown)
            {
                DoveMQMessage request = null;
                try
                {
                    request = incomingRequests.poll(1000, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }

                if (request != null)
                {
                    byte[] body = request.getPayload();
                    String payload = new String(body);
                    String replyToAddress = request.getMessageProperties().getReplyToAddress();
                    String messageId = request.getMessageProperties().getMessageId();
                    System.out.println("Received request: " + payload);

                    Producer producer = null;
                    synchronized (messageProducers)
                    {
                        /*
                         * Get the corresponding Producer, create one if
                         * not already in the map.
                         */
                        producer = messageProducers.get(replyToAddress);
                        if (producer == null)
                        {
                            producer = session.createProducer(replyToAddress);
                            messageProducers.put(replyToAddress, producer);
                        }
                    }

                    System.out.println("sending response to: " + replyToAddress + " for messageId: " + messageId);
                    DoveMQMessage response = MessageFactory.createMessage();
                    /*
                     * Set the response message's correlationId to be the same
                     * as the request message's messageId.
                     */
                    response.getMessageProperties().setCorrlelationId(messageId);
                    response.addPayload("Reply from consumer".getBytes());
                    producer.sendMessage(response);
                }
            }
        }

        void doShutdown()
        {
            shutdown = true;
        }

        private final Session session;
        private volatile boolean shutdown = false;
    }

    /**
     * Receive the request message and hand it off to RequestProcessor.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver
    {
        @Override
        public void messageReceived(DoveMQMessage message)
        {
            incomingRequests.add(message);
        }
    }

    public static void main(String[] args)
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                doShutdown = true;
            }
        });

        /*
         * Read the broker IP address passed in as -Ddovemq.broker
         * Defaults to localhost
         */
        String brokerIp = System.getProperty("dovemq.broker", "localhost");

        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initialize("rpcResponder");

        /*
         * Create an AMQP session.
         */
        Session session = ConnectionFactory.createSession(brokerIp);
        System.out.println("created session to DoveMQ broker running at: " + brokerIp);

        /*
         * Create a consumer and register a message receiver to
         * receive the request message from.
         */
        Consumer consumer = session.createConsumer(LISTEN_TO_ADDRESS);
        consumer.registerMessageReceiver(new SampleMessageReceiver());

        /*
         * Create a RequestProcessor to process incoming messages.
         */
        RequestProcessor requestProcessor = new RequestProcessor(session);
        new Thread(requestProcessor).start();

        System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
        while (!doShutdown)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        requestProcessor.doShutdown();

        /*
         * Close the AMQP session
         */
        session.close();

        /*
         * Shutdown DoveMQ runtime.
         */
        ConnectionFactory.shutdown();
    }
}
