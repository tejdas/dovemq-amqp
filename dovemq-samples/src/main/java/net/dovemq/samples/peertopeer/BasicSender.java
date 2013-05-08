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
 * This sample shows how to create a DoveMQ producer that creates a
 * queue in the DoveMQ broker, and sends messages.
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
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initializeClientEndpoint("sender", new SampleEndpointListener());

        try {
            /*
             * Create an AMQP session.
             */
            Connection amqpConnection = ConnectionFactory.createConnectionToAMQPEndpoint("localhost", 8746);
            Session session = amqpConnection.createSession();
            System.out.println("created session to DoveMQ broker running at: " + "localhost");

            /*
             * Create a producer that binds to a queue on the broker.
             */
            Sender sender = session.createSender("foobar");

            /*
             * Create and send some messages.
             */
            for (int i = 0; i < 1; i++) {
                DoveMQMessage message = MessageFactory.createMessage();
                String msg = "Hello from Producer: msg: " + i;
                System.out.println("sending message: " + msg);
                message.addPayload(msg.getBytes());
                sender.sendMessage(message);
            }

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
