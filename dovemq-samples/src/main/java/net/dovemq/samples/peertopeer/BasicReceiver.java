package net.dovemq.samples.peertopeer;

import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;
import net.dovemq.api.RecvEndpoint;
import net.dovemq.api.RecvEndpointListener;
import net.dovemq.api.Sender;

/**
 * This sample shows how to create a DoveMQ consumer that creates a
 * queue in the DoveMQ broker, and waits for incoming messages.
 */
public class BasicReceiver {
    private static final SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
    private static volatile RecvEndpoint recvEndpoint = null;


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

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Sender sender = recvEndpoint.getSession().createSender("ReplyTarget");
                    try {
                        Thread.sleep(3000);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    DoveMQMessage response = MessageFactory.createMessage();
                    String responsemsg = "Hello Response from receiver";
                    System.out.println("sending message: " + responsemsg);
                    response.addPayload(responsemsg.getBytes());
                    sender.sendMessage(response);
                }
            };

            new Thread(r).start();
        }
    }

    private static class SampleEndpointListener implements RecvEndpointListener {
        @Override
        public void recvEndpointCreated(RecvEndpoint recvEndpoint) {
            BasicReceiver.recvEndpoint = recvEndpoint;
            System.out.println("recv endpoint created: " + recvEndpoint.getTargetName());
            recvEndpoint.registerMessageReceiver(messageReceiver);
        }
    }

    public static void main(String[] args) {
        String listenPortAsString = System.getProperty("dovemq.listenPort");
        if (listenPortAsString == null) {
            System.out.println("Please provide the listenPort: -Ddovemq.listenPort");
            return;
        }

        int listenPort = Integer.valueOf(listenPortAsString);
        /*
         * Initialize the DoveMQ runtime, specifying an endpoint name.
         */
        ConnectionFactory.initializeEndpoint("consumer", listenPort, new SampleEndpointListener());

        try {
            System.out.println("waiting for messages. Press Ctl-C to shut down consumer.");
            /*
             * Register a shutdown hook to perform graceful shutdown.
             */
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    /*
                     * Shutdown DoveMQ runtime.
                     */
                    ConnectionFactory.shutdown();
                }
            });
        }
        catch (Exception ex) {
            System.out.println("Caught Exception: " + ex.toString());
            /*
             * Shutdown DoveMQ runtime.
             */
            ConnectionFactory.shutdown();
        }
    }
}
