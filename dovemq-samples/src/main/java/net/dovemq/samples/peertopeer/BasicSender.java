package net.dovemq.samples.peertopeer;

import net.dovemq.api.Connection;
import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.ChannelEndpoint;
import net.dovemq.api.ChannelEndpointListener;
import net.dovemq.api.Channel;
import net.dovemq.api.Session;

/**
 * AMQP is a peer-to-peer messaging transport protocol. DoveMQ provides capability
 * for AMQP to be used in a peer-to-peer manner. In this case, there is no Broker
 * involved. Messages are sent between two AMQP peers. For DoveMQ to work in the
 * peer-to-peer manner, one endpoint acts as a DoveMQ listener, listening on a TCP port.
 * Another endpoint acts as a client. After an AMQP session is established, there is
 * no distinction between the client, and listener, i.e, they become peers and either
 * endpoint can initiate an AMQP link on the underlying (bidirectional) AMQP session.
 *
 * This sample demonstrates how to create and use a DoveMQ channel.
 */
public class BasicSender {
    private static final String CHANNEL_ENDPOINT_NAME = "SampleChannelEndpoint";

    private static final SampleMessageReceiver messageReceiver = new SampleMessageReceiver();

    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the Consumer.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        @Override
        public void messageReceived(DoveMQMessage message) {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message: " + payload);
        }
    }

    private static class SampleEndpointListener implements ChannelEndpointListener {
        @Override
        public void channelCreated(ChannelEndpoint channelEndpoint) {
            System.out.println("Channel endpoint created: " + channelEndpoint.getTargetName());
            channelEndpoint.registerMessageReceiver(messageReceiver);
        }
    }

    public static void main(String[] args) {
        /*
         * Read the broker IP address passed in as -Ddovemq.broker Defaults to
         * localhost
         */
        String doveMQListenerIP = System.getProperty("dovemq.endpoint", "localhost");

        String connectPortAsString = System.getProperty("dovemq.endpointPort");
        if (connectPortAsString == null) {
            System.out.println("Please provide the endpoint port: -Ddovemq.endpointPort");
            return;
        }

        int connectPort = Integer.valueOf(connectPortAsString);
        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initializeClientEndpoint("ChannelSender", new SampleEndpointListener());

        try {
            /*
             * Create an AMQP session.
             */
            Connection amqpConnection = ConnectionFactory.createConnectionToAMQPEndpoint(doveMQListenerIP, connectPort);
            Session session = amqpConnection.createSession();
            System.out.println("created session to DoveMQ broker running at: " + "localhost");

            Channel sender = session.createChannel(CHANNEL_ENDPOINT_NAME);

            /*
             * Create and send a message.
             */
            DoveMQMessage message = MessageFactory.createMessage();
            String msg = "Hello from Producer";
            System.out.println("sending message: " + msg);
            message.addPayload(msg.getBytes());
            message.getMessageProperties().setReplyToAddress("ReplyToEndpoint");
            sender.sendMessage(message);

            System.out.println("sleeping for 10 secs to receive the message.");
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
