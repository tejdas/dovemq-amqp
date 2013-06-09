package net.dovemq.samples.peertopeer;

import net.dovemq.api.Channel;
import net.dovemq.api.ChannelEndpoint;
import net.dovemq.api.ChannelEndpointListener;
import net.dovemq.api.ConnectionFactory;
import net.dovemq.api.DoveMQMessage;
import net.dovemq.api.DoveMQMessageReceiver;
import net.dovemq.api.MessageFactory;

/**
 * AMQP is a peer-to-peer messaging transport protocol. DoveMQ provides capability
 * for AMQP to be used in a peer-to-peer manner. In this case, there is no Broker
 * involved. Messages are sent between two AMQP peers. For DoveMQ to work in the
 * peer-to-peer manner, one endpoint acts as a DoveMQ listener, listening on a TCP port.
 * Another endpoint acts as a client. After an AMQP session is established, there is
 * no distinction between the client, and listener, i.e, they become peers and either
 * endpoint can initiate an AMQP link on the underlying (bidirectional) AMQP session.
 *
 * This sample demonstrates how to create and use a DoveMQ listener.
 */
public class BasicReceiver {
    private static final SampleMessageReceiver messageReceiver = new SampleMessageReceiver();
    private static volatile ChannelEndpoint channelEndpoint = null;


    /**
     * Implementation of a sample MessageReceiver callback, that is registered
     * with the ChannelEndpoint.
     */
    private static class SampleMessageReceiver implements DoveMQMessageReceiver {
        @Override
        public void messageReceived(DoveMQMessage message) {
            byte[] body = message.getPayload();
            String payload = new String(body);
            System.out.println("Received message: " + payload);

            final String replyToEndpoint = message.getMessageProperties().getReplyToAddress();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Channel sender = channelEndpoint.getSession().createChannel(replyToEndpoint);
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

    private static class SampleEndpointListener implements ChannelEndpointListener {
        @Override
        public void channelCreated(ChannelEndpoint channelEndpoint) {
            BasicReceiver.channelEndpoint = channelEndpoint;
            System.out.println("Channel endpoint created: " + channelEndpoint.getTargetName());
            channelEndpoint.registerMessageReceiver(messageReceiver);
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
        ConnectionFactory.initializeEndpoint("BasicChannelEndpoint", listenPort, new SampleEndpointListener());

        try {
            System.out.println("waiting for messages. Press Ctl-C to shut down endpoint.");
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
