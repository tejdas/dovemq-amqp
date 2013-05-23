package net.dovemq.samples.peertopeer;

import net.dovemq.api.Connection;
import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.RecvEndpoint;
import net.dovemq.api.RecvEndpointListener;
import net.dovemq.api.Sender;
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
 * This sample demonstrates how to create and use a DoveMQ client.
 */
public class BasicSender {
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

    private static class SampleEndpointListener implements RecvEndpointListener {
        @Override
        public void recvEndpointCreated(RecvEndpoint recvEndpoint) {
            System.out.println("recv endpoint created: " + recvEndpoint.getTargetName());
            recvEndpoint.registerMessageReceiver(messageReceiver);
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
        ConnectionFactory.initializeClientEndpoint("sender", new SampleEndpointListener());

        try {
            /*
             * Create an AMQP session.
             */
            Connection amqpConnection = ConnectionFactory.createConnectionToAMQPEndpoint(doveMQListenerIP, connectPort);
            Session session = amqpConnection.createSession();
            System.out.println("created session to DoveMQ broker running at: " + "localhost");

            Sender sender = session.createSender("sampleTarget");

            /*
             * Create and send a message.
             */
            DoveMQMessage message = MessageFactory.createMessage();
            String msg = "Hello from Producer";
            System.out.println("sending message: " + msg);
            message.addPayload(msg.getBytes());
            sender.sendMessage(message);

            System.out.println("sleeping for 10 secs");
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
