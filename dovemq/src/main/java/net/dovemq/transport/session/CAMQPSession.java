package net.dovemq.transport.session;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

import net.dovemq.transport.session.CAMQPSessionManager;
import net.dovemq.transport.connection.CAMQPConnection;
import net.dovemq.transport.connection.CAMQPIncomingChannelHandler;
import net.dovemq.transport.frame.CAMQPFrame;
import net.dovemq.transport.frame.CAMQPFrameConstants;
import net.dovemq.transport.frame.CAMQPFrameHeader;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.CAMQPSyncDecoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlBegin;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlEnd;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;

@ThreadSafe
class CAMQPSession implements CAMQPIncomingChannelHandler, CAMQPSessionInterface, Runnable
{
    private static class Transfer
    {
        Transfer(CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender)
        {
            super();
            this.transferFrame = transferFrame;
            this.payload = payload;
            this.linkSender = linkSender;
        }
        final CAMQPControlTransfer transferFrame;
        final CAMQPMessagePayload payload;
        final CAMQPLinkSenderInterface linkSender;
    }
    
    static class CAMQPFlowFrameSender implements Runnable
    {
        private final CAMQPSession session;
        CAMQPFlowFrameSender(CAMQPSession session)
        {
            super();
            this.session = session;
        }

        @Override
        public void run()
        {
            session.sendFlowInternal();         
        }
    }
    
    static class CAMQPChannel
    {
        final CAMQPConnection getAmqpConnection()
        {
            return amqpConnection;
        }
        final int getChannelId()
        {
            return channelId;
        }
        CAMQPChannel(CAMQPConnection amqpConnection, int channelId)
        {
            super();
            this.amqpConnection = amqpConnection;
            this.channelId = channelId;
        }
        private final CAMQPConnection amqpConnection;
        private final int channelId;
    }
    
    private static final Logger log = Logger.getLogger(CAMQPSession.class);
    
    /*
     * The following attributes are used to schedule a recurring send of flow-frame
     * until the session is not under flow-control state any longer.
     */
    private static final long FLOW_SENDER_INTERVAL = 1000L;
    private final ScheduledExecutorService flowSendScheduler = Executors.newScheduledThreadPool(1);
    @GuardedBy("this")
    private Date lastFlowSent = new Date();
    @GuardedBy("this")
    private boolean isFlowSendScheduled = false;
    
    private final Map<Long, CAMQPLinkMessageHandler> linkReceivers = new ConcurrentHashMap<Long, CAMQPLinkMessageHandler>();

    private CAMQPSessionStateActor stateActor = null;
    @GuardedBy("stateActor")
    private boolean closePending = false;

    /*
     * AMQP Connection/channel state
     */
    @GuardedBy("stateActor")
    private int outgoingChannelNumber = 0;
    @GuardedBy("stateActor")
    private int incomingChannelNumber = 0;
    @GuardedBy("stateActor")
    private CAMQPConnection connection = null;

    @GuardedBy("stateActor")
    CAMQPConnection getConnection()
    {
        return connection;
    }

    /*
     * I/O state
     */
    private AtomicLong deliveryId = new AtomicLong(0L);
    
    @GuardedBy("this")
    private boolean sendInProgress = false;
    @GuardedBy("this")
    private ConcurrentLinkedQueue<Transfer> unsentTransfers = new ConcurrentLinkedQueue<Transfer>();

    /*
     * Flow control state
     */
    @GuardedBy("this")
    private long nextOutgoingTransferId = 0;
    @GuardedBy("this")
    private long outgoingWindow = CAMQPSessionConstants.DEFAULT_OUTGOING_WINDOW_SIZE;
    @GuardedBy("this")
    private long incomingWindow = CAMQPSessionConstants.DEFAULT_INCOMING_WINDOW_SIZE;
    
    /*
     * computed after begin, transfer and flow frames received from peer
     */
    @GuardedBy("this")
    private long remoteOutgoingWindow = -1;
    @GuardedBy("this")
    private long nextIncomingTransferId = -1;
    @GuardedBy("this")
    private long remoteIncomingWindow = -1;

    /*
     * Called during passive session attach
     */
    CAMQPSession(CAMQPConnection connection, CAMQPSessionStateActor stateActor)
    {
        super();
        this.connection = connection;
        this.stateActor = stateActor;
    }

    /*
     * Called during active session attach
     */
    CAMQPSession()
    {
        super();
        stateActor = new CAMQPSessionStateActor(this);
    }

    public void transferExecuteFailed(long failedTransferId)
    {
        CAMQPDefinitionError error = new CAMQPDefinitionError();
        error.setDescription("failed to execute"); // TODO
        closeInternal(error);
    }
    
    public void open(String targetContainerId) throws CAMQPSessionBeginException
    {
        begin(targetContainerId);
    }

    private void begin(String targetContainerId) throws CAMQPSessionBeginException
    {
        connection = CAMQPSessionManager.getCAMQPConnection(targetContainerId);
        /*
         * In the case of session initiator, a. reserve a txChannel from
         * underlying CAMQPConnection b. register with CAMQPSessionFrameHandler
         * that will then dispatch the incoming BEGIN control. The BEGIN
         * control cannot be directly dispatched to the session because the
         * session's rxChannel is not known yet, and hence not registered with
         * the CAMQPConnection.
         */
        outgoingChannelNumber = connection.reserveOutgoingChannel();
        CAMQPSessionFrameHandler.getSingleton().registerSessionHandshakeInProgress(outgoingChannelNumber, this);

        CAMQPControlBegin beginControl = new CAMQPControlBegin();

        setFlowControlAttributes(beginControl);
        CAMQPSessionControlWrapper beginContext = new CAMQPSessionControlWrapper(outgoingChannelNumber, beginControl);

        stateActor.sendBegin(beginContext);
        stateActor.waitForMapped();
    }
    
    private synchronized void setFlowControlAttributes(CAMQPControlBegin beginControl)
    {
        beginControl.setIncomingWindow(incomingWindow);
        beginControl.setOutgoingWindow(outgoingWindow);
        beginControl.setNextOutgoingId(nextOutgoingTransferId);        
    }
    
    /*
     * Attach received by the Session acceptor
     */
    void beginReceived(CAMQPSessionControlWrapper receivedData)
    {
        /*
         * In the case of session initiator's peer: a. Get the rxChannel from
         * FrameHeader and register with underlying CAMQPConnection b. reserve a
         * txChannel from underlying CAMQPConnection
         */
        incomingChannelNumber = receivedData.getChannelNumber();
        outgoingChannelNumber = connection.reserveOutgoingChannel();
        connection.register(incomingChannelNumber, this);
        
        CAMQPControlBegin peerBeginControl = (CAMQPControlBegin) receivedData.getSessionControl();
        retrieveRemoteFlowControlAttributes(peerBeginControl);

        CAMQPControlBegin beginControl = new CAMQPControlBegin();
        beginControl.setRemoteChannel(incomingChannelNumber);
        setFlowControlAttributes(beginControl);
        CAMQPSessionControlWrapper beginContext = new CAMQPSessionControlWrapper(outgoingChannelNumber, beginControl);

        stateActor.sendBegin(beginContext);
    }
    
    private synchronized void retrieveRemoteFlowControlAttributes(CAMQPControlBegin peerBeginControl)
    {
        retrieveAndSetRemoteFlowControlAttributes(peerBeginControl.getOutgoingWindow(),
                peerBeginControl.getNextOutgoingId(), peerBeginControl.getIncomingWindow());
    }
    
    @GuardedBy("this")
    void retrieveAndSetRemoteFlowControlAttributes(long remoteOutgoingWindow, long nextIncomingTransferId, long remoteIncomingWindow)
    {
        this.remoteOutgoingWindow = remoteOutgoingWindow;
        this.nextIncomingTransferId = nextIncomingTransferId;
        this.remoteIncomingWindow = remoteIncomingWindow;
    }
    
    /*
     * Begin received by the Session initiator
     */
    void beginResponse(CAMQPControlBegin peerBeginControl, CAMQPFrameHeader frameHeader)
    {
        /*
         * In the case of session initiator: Get the rxChannel from FrameHeader
         * and register with underlying CAMQPConnection
         */
        incomingChannelNumber = frameHeader.getChannelNumber();
        retrieveRemoteFlowControlAttributes(peerBeginControl);
        
        connection.register(incomingChannelNumber, this);
        CAMQPSessionControlWrapper beginContext = new CAMQPSessionControlWrapper(outgoingChannelNumber, peerBeginControl);
        stateActor.beginReceived(beginContext);
    }

    void mapped()
    {
        CAMQPSessionManager.sessionCreated(connection.getRemoteContainerId(), outgoingChannelNumber, this);
        String logInfo = String.format("Session is attached to txChannel: %d and rxChannel: %d", outgoingChannelNumber, incomingChannelNumber);
        log.info(logInfo);
    }

    public void close()
    {
        closeInternal(null);
    }

    private void closeInternal(CAMQPDefinitionError error)
    {
        synchronized (stateActor)
        {
            if (!isSessionActive())
            {
                log.info("Session already closed or closing in progress");
                return;
            }
                
            closePending = true;
        }
        log.debug("Sleeping for 1 sec before session close");
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        CAMQPControlEnd endControl = new CAMQPControlEnd();
        if (error != null)
        {
            endControl.setError(error);
        }
        CAMQPSessionControlWrapper endContext = new CAMQPSessionControlWrapper(outgoingChannelNumber, endControl);
        stateActor.sendEnd(endContext);
        stateActor.waitForUnmapped();
    }
    
    void endReceived(CAMQPControlEnd receivedData)
    {
        CAMQPControlEnd endControl = new CAMQPControlEnd();
        CAMQPSessionControlWrapper endContext = new CAMQPSessionControlWrapper(outgoingChannelNumber, endControl);
        stateActor.sendEnd(endContext);
    }

    void unmapped()
    {
        CAMQPSessionManager.sessionClosed(connection.getRemoteContainerId(), outgoingChannelNumber);
        connection.detach(outgoingChannelNumber, incomingChannelNumber);
        synchronized (stateActor)
        {
            connection = null;
            outgoingChannelNumber = 0;
            incomingChannelNumber = 0;
        }

        flowSendScheduler.shutdown();
        
        for (Long linkHandle : linkReceivers.keySet())
        {
            CAMQPLinkMessageHandler linkReceiver = linkReceivers.get(linkHandle);
            if (linkReceiver != null)
                linkReceiver.sessionClosed();
        }
    }

    @Override
    public void channelAbruptlyDetached()
    {
        log.warn("Session abruptly closed");
        stateActor.channelAbruptlyDetached();
    }
    
    @Override
    public void sendLinkControlFrame(ChannelBuffer encodedLinkControlFrame)
    {
        CAMQPChannel channel = getChannel();
        if (channel != null)
        {
            channel.getAmqpConnection().sendFrame(encodedLinkControlFrame, channel.getChannelId());;
        }   
    }
    

    @Override
    public long getNextDeliveryId()
    {
        return deliveryId.getAndIncrement();
    }
    
    @GuardedBy("this")
    private boolean canSendTransfer()
    {
        return (!unsentTransfers.isEmpty()) && (remoteIncomingWindow > 0);
    }
    
    @Override
    public void sendTransfer(CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload, CAMQPLinkSenderInterface linkSender)
    {
        CAMQPControlFlow flow = null;
        CAMQPChannel channel = getChannel();
        if (channel == null)
        {
            throw new CAMQPSessionClosedException("Underlying channel is detached");
        }
        
        /*
         * TODO: if unsentTransfers.add() fails, throw an exception
         * indicating to the Link layer that the outgoing-window is full
         */
        synchronized (this)
        {
            if (!unsentTransfers.isEmpty())
            {
                assert(sendInProgress || (remoteIncomingWindow <= 0));
                unsentTransfers.add(new Transfer(transferFrame, payload, linkSender));
                return;
            }

            if (sendInProgress || (remoteIncomingWindow <= 0))
            {
                unsentTransfers.add(new Transfer(transferFrame, payload, linkSender));
                return;
            }
           
            sendInProgress = true;
            nextOutgoingTransferId++;
            remoteIncomingWindow--;
            if (remoteIncomingWindow < CAMQPSessionConstants.MIN_INCOMING_WINDOW_SIZE_THRESHOLD)
            {
                flow = createFlowFrameIfNotScheduled();
            }
        }
        
        sendTransferFrame(transferFrame, payload, channel);
        linkSender.messageSent(transferFrame);
        
        if (flow != null)
        {
            sendFlowFrame(flow, channel);
            flowSendScheduler.schedule(new CAMQPFlowFrameSender(this), FLOW_SENDER_INTERVAL, TimeUnit.MILLISECONDS);
        }
           
        synchronized (this)
        {
            /*
             * There are more transfer frames in the outgoing window waiting
             * to be sent. Schedule a runnable to send the transfer frames
             * only if we are not under flow control
             */
            if (!canSendTransfer())
            {
                sendInProgress = false;
                return;
            }
        }
   
        CAMQPSessionManager.getExecutor().execute(this);
    }

    /**
     * Called by Link Layer to send a flow frame
     */
    @Override
    public void sendFlow(CAMQPControlFlow flow)
    {
        CAMQPChannel channel = getChannel();
        if (channel != null)
        {
            synchronized (this)
            {
                lastFlowSent = new Date();
                populateFlowFrame(flow);
            }
            sendFlowFrame(flow, channel);
        }
    }
    
    @GuardedBy("this")
    private CAMQPControlFlow createFlowFrameIfNotScheduled()
    {
        CAMQPControlFlow flow = null;
        if (!isFlowSendScheduled)
        {
            isFlowSendScheduled = true;
            flow = new CAMQPControlFlow();
            populateFlowFrame(flow);
            flow.setEcho(true);
            lastFlowSent = new Date();
        }
        return flow;
    }
    
    /*
     * Flow frame is sent in the following cases:
     * 
     * From Link layer: sendFlow() : send right away : do not schedule a flow resend.
     * 
     * From session layer: in response to peer's flow frame (echo flag true) : send right away : do not schedule a a flow resend.
     * 
     * From session layer: after detecting a flow control: send only if the last send was done before FLOW_SENDER_INTERVAL. Also
     * schedule for a flow-frame send after FLOW_SENDER_INTERVAL.
     * 
     * From session layer : a scheduled timer :
     *      If a flow control frame has been sent in the past within FLOW_SENDER_INTERVAL,  do not send flow control frame, but schedule
     *      another one in the future after time left.
     *      If it is not under flow control anymore, do not send flow control.
            Otherwise send and schedule for a flow-frame send after FLOW_SENDER_INTERVAL.
     *
     * Do NOT schedule sending of another flow-frame, if acting as a Session Receiver.
     */
    private void sendFlowInternal()
    {
        CAMQPChannel channel = getChannel();
        if (channel == null)
        {
            return;
        }
        CAMQPControlFlow flow = null;
        long scheduleAfter = FLOW_SENDER_INTERVAL;
        synchronized (this)
        {
            /*
             * Session is not under flow-control on either side, so no need to send flow frame
             */
            if (!isFlowSendScheduled)
                log.error("Assert failed isFlowSendScheduled");
            
            if (!isUnderFlowControl())
            {
                isFlowSendScheduled = false;
                return;
            }
 
            Date now = new Date();
            if ((now.getTime() - lastFlowSent.getTime()) < FLOW_SENDER_INTERVAL)
            {
                // still too early to send a flow frame
                scheduleAfter = FLOW_SENDER_INTERVAL - (now.getTime() - lastFlowSent.getTime());
            }
            else
            {
                flow = new CAMQPControlFlow();
                populateFlowFrame(flow);
                flow.setEcho(true);
                lastFlowSent = now;
            }
        }
        
        if (flow != null)
            sendFlowFrame(flow, channel);
        
        flowSendScheduler.schedule(new CAMQPFlowFrameSender(this), scheduleAfter, TimeUnit.MILLISECONDS);
    }
    
    @GuardedBy("this")
    private boolean isUnderFlowControl()
    {
        return (!unsentTransfers.isEmpty() && remoteIncomingWindow < CAMQPSessionConstants.MIN_INCOMING_WINDOW_SIZE_THRESHOLD);
    }

    @Override
    public void run()
    {
        while (true)
        {
            Transfer transfer = null;
            CAMQPControlFlow flow = null;
            CAMQPChannel channel = getChannel();
            
            synchronized (this)
            {
                if ((channel != null) && canSendTransfer())
                {
                    transfer = unsentTransfers.poll();
                    nextOutgoingTransferId++;
                    remoteIncomingWindow--;
                    if (isUnderFlowControl())
                    {
                        flow = createFlowFrameIfNotScheduled();
                    }
                }
                else
                {
                    sendInProgress = false;
                    return;
                }
            }

            sendTransferFrame(transfer.transferFrame, transfer.payload, channel);
            transfer.linkSender.messageSent(transfer.transferFrame);
            if (flow != null)
            {
                sendFlowFrame(flow, channel);
                flowSendScheduler.schedule(new CAMQPFlowFrameSender(this), FLOW_SENDER_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    @GuardedBy("this")
    private void populateFlowFrame(CAMQPControlFlow flow)
    {
        flow.setOutgoingWindow(outgoingWindow);
        flow.setIncomingWindow(incomingWindow);
        flow.setNextOutgoingId(nextOutgoingTransferId);
        flow.setNextIncomingId(nextIncomingTransferId);
    }
    
    private void sendFlowFrame(CAMQPControlFlow flow, CAMQPChannel channel)
    {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlFlow.encode(encoder, flow);
        ChannelBuffer encodedTransfer = encoder.getEncodedBuffer();
        channel.getAmqpConnection().sendFrame(encodedTransfer, channel.getChannelId());
    }
    
    private void sendTransferFrame(CAMQPControlTransfer transfer, CAMQPMessagePayload payload, CAMQPChannel channel)
    {
        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlTransfer.encode(encoder, transfer);
        encoder.writePayload(payload);
        ChannelBuffer encodedTransfer = encoder.getEncodedBuffer();
        channel.getAmqpConnection().sendFrame(encodedTransfer, channel.getChannelId());
    }
    
    private CAMQPChannel getChannel()
    {
        synchronized (stateActor)
        {
            if (isSessionActive())
            {
                return new CAMQPChannel(connection, outgoingChannelNumber);
            }
            return null;
        }
    }
    
    @GuardedBy("stateActor")
    private boolean isSessionActive()
    {
        return ((connection != null) && (!closePending) && (stateActor.getCurrentState() == State.MAPPED));
    }

    @Override
    public void frameReceived(CAMQPFrame frame)
    {
        if (frame.getHeader().getFrameType() == CAMQPFrameConstants.AMQP_FRAME_TYPE)
        {
            CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
            decoder.take(frame.getBody());
            String controlName = decoder.readSymbol();
            if (isSessionControlFrame(controlName))
            {
                processSessionControlFrame(controlName, frame);
            }
            else if (isLinkControlFrame(controlName))
            {
                processLinkControlFrame(controlName, frame);
            }
            else if (isFlowFrame(controlName))
            {
                processFlowFrame(frame);
            }
            else if (isTransferFrame(controlName))
            {
                processTransferFrame(controlName, frame);
            }
            else
            {
                // TODO handle error condition
            }
        }
        else
        {
            //linkReceiver.customFrameReceived(frame);
        }
    }

    private boolean isSessionControlFrame(String controlName)
    {
        return ((controlName.equalsIgnoreCase(CAMQPControlBegin.descriptor)) || (controlName.equalsIgnoreCase(CAMQPControlEnd.descriptor)));
    }
    
    private boolean isLinkControlFrame(String controlName)
    {
        return ((controlName.equalsIgnoreCase(CAMQPControlAttach.descriptor)) || (controlName.equalsIgnoreCase(CAMQPControlDetach.descriptor)));
    }
    
    private boolean isFlowFrame(String controlName)
    {
        return (controlName.equalsIgnoreCase(CAMQPControlFlow.descriptor));
    }

    private boolean isTransferFrame(String controlName)
    {
        return (controlName.equalsIgnoreCase(CAMQPControlTransfer.descriptor));
    }
    
    private void processFlowFrame(CAMQPFrame frame)
    {
        ChannelBuffer body = frame.getBody();
        if (body == null)
        {
            return;
        }
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(body);
        CAMQPControlFlow flowFrame = CAMQPControlFlow.decode(decoder);
        
        boolean unsentTransferFramesPending = false;
        CAMQPControlFlow echoedFlowFrame = null;
        
        CAMQPChannel channel = (needFlowFrameEcho(flowFrame))? getChannel() : null;
        synchronized (this)
        {
            if (flowFrame.getNextOutgoingId() > nextIncomingTransferId)
                nextIncomingTransferId = flowFrame.getNextOutgoingId();
 
            remoteOutgoingWindow = flowFrame.getOutgoingWindow();
            
            remoteIncomingWindow =
                flowFrame.getIncomingWindow() - (nextOutgoingTransferId - flowFrame.getNextIncomingId());
            
            if (!sendInProgress && canSendTransfer())
            {
                sendInProgress = true;
                unsentTransferFramesPending = true;
            }
            
            /*
             * Send back a echo Flow frame if it is a session Flow only.
             * Otherwise, let the Link layer send it.
             */
            if (channel != null)
            {
                lastFlowSent = new Date();
                echoedFlowFrame = new CAMQPControlFlow();
                populateFlowFrame(echoedFlowFrame);
                echoedFlowFrame.setEcho(false); // we do not want echo flow frame ping-pong
            }
        }
       
        if (echoedFlowFrame != null)
        {
            sendFlowFrame(echoedFlowFrame, channel);
        }

        if (flowFrame.isSetHandle())
        {
            // dispatch to a LinkReceiver
            CAMQPLinkMessageHandler linkReceiver = linkReceivers.get(flowFrame.getHandle());
            if (linkReceiver != null)
                linkReceiver.flowReceived(flowFrame);
        }
        
        if (unsentTransferFramesPending)
        {
            CAMQPSessionManager.getExecutor().execute(this);
        }
    }
    
    private boolean needFlowFrameEcho(CAMQPControlFlow flowFrame)
    {
        return (flowFrame.isSetEcho() && flowFrame.getEcho() && (!flowFrame.isSetHandle()));
    }
    
    private void processTransferFrame(String controlName, CAMQPFrame frame)
    {
        ChannelBuffer body = frame.getBody();
        if (body == null)
        {
            return;
        }
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(body);
        CAMQPControlTransfer transferFrame = CAMQPControlTransfer.decode(decoder);
        CAMQPMessagePayload payload = decoder.getPayload();
        
        CAMQPControlFlow flow = null;
        synchronized (this)
        {
            if (incomingWindow <= 0)
            {
                /*
                 * TODO
                 * Peer is not honoring the session flow control.
                 * Reject the incoming transfer frame and close the session
                 */
                log.warn("CAMQPSession.processTransferFrame(): received transfer frames with incomingWindow :" + incomingWindow);
            }
            else
            {
                remoteOutgoingWindow--;
                incomingWindow--;
            }
            
            if ((incomingWindow < CAMQPSessionConstants.MIN_INCOMING_WINDOW_SIZE_THRESHOLD) ||
                (remoteOutgoingWindow < CAMQPSessionConstants.MIN_INCOMING_WINDOW_SIZE_THRESHOLD))
            {
                flow = createFlowFrameIfNotScheduled();
            }
        }

        if (flow != null)
        {
            CAMQPChannel channel = getChannel();
            if (channel != null)
                sendFlowFrame(flow, channel);
            
            /*
             * Do NOT schedule sending of another flow-frame, if acting as a Session Receiver.
             */
        }
        
        // dispatch to a LinkReceiver
        CAMQPLinkMessageHandler linkReceiver = linkReceivers.get(transferFrame.getHandle());
        if (linkReceiver != null)
            linkReceiver.transferReceived(transferFrame.getDeliveryId(), transferFrame, payload);
        
        return;
    }

    private void processLinkControlFrame(String controlName, CAMQPFrame frame)
    {
        ChannelBuffer body = frame.getBody();
        if (body == null)
        {
            return;
        }
        
        CAMQPLinkMessageHandler linkReceiver = null;
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(body);
        if (controlName.equalsIgnoreCase(CAMQPControlAttach.descriptor))
        {
            CAMQPControlAttach data = CAMQPControlAttach.decode(decoder);
            linkReceiver = CAMQPSessionManager.getLinkReceiverFactory().createLinkReceiver(this, data);
            linkReceivers.put(data.getHandle(), linkReceiver);
            linkReceiver.attachReceived(data);
        }
        else if (controlName.equalsIgnoreCase(CAMQPControlDetach.descriptor))
        {
            CAMQPControlDetach data = CAMQPControlDetach.decode(decoder);
            linkReceiver = linkReceivers.get(data.getHandle());
            if (linkReceiver != null)
            {
                linkReceiver.detachReceived(data);
            }
        }
    }
    
    private void processSessionControlFrame(String controlName, CAMQPFrame frame)
    {
        CAMQPFrameHeader frameHeader = frame.getHeader();
        ChannelBuffer body = frame.getBody();

        if (body == null)
        {
            return;
        }
        incomingChannelNumber = frameHeader.getChannelNumber();
        CAMQPSyncDecoder decoder = CAMQPSyncDecoder.createCAMQPSyncDecoder();
        decoder.take(body);
        if (controlName.equalsIgnoreCase(CAMQPControlEnd.descriptor))
        {
            CAMQPControlEnd data = CAMQPControlEnd.decode(decoder);
            stateActor.endReceived(data);
        }
    }

    @Override
    public void registerLinkReceiver(Long linkHandle, CAMQPLinkMessageHandler linkReceiver)
    {
        linkReceivers.put(linkHandle, linkReceiver);   
    }

    @Override
    public void ackTransfer(long transferId)
    {
        synchronized (this)
        {
            incomingWindow++;
        }      
    }
}
