package net.dovemq.transport.link;

import java.math.BigInteger;
import java.util.UUID;

import net.jcip.annotations.GuardedBy;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.protocol.data.CAMQPDefinitionSource;
import net.dovemq.transport.protocol.data.CAMQPDefinitionTarget;
import net.dovemq.transport.session.CAMQPSessionInterface;

/**
 * This class is extended by Link Sender and Link Receiver
 * implementations, to share the common logic around link
 * establishment, teardown and some common flow-control
 * attributes.
 * 
 * @author tejdas
 */
abstract class CAMQPLinkEndpoint implements CAMQPLinkMessageHandler
{
    private final boolean role;
    private String linkName;
    private final CAMQPLinkStateActor linkStateActor;
    
    protected long linkHandle;
    
    private String sourceAddress;
    private String targetAddress;
    
    /*
     * Flow-control attributes
     */
    protected long deliveryCount = 0;
    protected long linkCredit = 0;
    protected long available = 0;

    /**
     * @param role: true for link receiver
     *              false for link sender
     */
    public CAMQPLinkEndpoint(boolean role)
    {
        super();
        this.role = role;
        linkStateActor = new CAMQPLinkStateActor(this);
    }
 
    /**
     * Establishes an AMQP link to a remote AMQP end-point.
     * @param sourceName
     * @param targetName
     */
    void createLink(String sourceName, String targetName)
    {
        sourceAddress = sourceName;
        targetAddress = targetName;

        linkHandle = CAMQPLinkManager.getNextLinkHandle();
        linkName = UUID.randomUUID().toString();
        CAMQPLinkManager.registerOutstandingLink(linkName, this);
        
        CAMQPControlAttach data = new CAMQPControlAttach();
        data.setHandle(linkHandle);
        data.setName(linkName);
        data.setRole(role);
        
        CAMQPDefinitionSource source = new CAMQPDefinitionSource();
        source.setAddress(sourceAddress);
        data.setSource(source);
        
        CAMQPDefinitionTarget target = new CAMQPDefinitionTarget();
        target.setAddress(targetAddress);
        data.setTarget(target);
     
        source.setDynamic(false);
        if (role == CAMQPLinkConstants.ROLE_SENDER)
        {
            data.setInitialDeliveryCount(deliveryCount);
        }
        data.setMaxMessageSize(BigInteger.valueOf(CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE));
        
        linkStateActor.sendAttach(data);
        linkStateActor.waitForAttached();
    }
    
    void resumeLink()
    {
        
    }
 
    /**
     * Closes an AMQP link
     */
    void destroyLink()
    {
        CAMQPControlDetach data = new CAMQPControlDetach();
        data.setClosed(true);
        data.setHandle(linkHandle);
        linkStateActor.sendDetach(data);
        linkStateActor.waitForDetached();
    }
    
    /**
     * Closes an AMQP link, specifying the reason
     * for closure.
     * @param message
     */
    void destroyLink(String message)
    {
        CAMQPControlDetach data = new CAMQPControlDetach();
        CAMQPDefinitionError error = new CAMQPDefinitionError();
        error.setCondition(message);       
        data.setError(error);
        data.setClosed(true);
        data.setHandle(linkHandle);
        linkStateActor.sendDetach(data);
        linkStateActor.waitForDetached();
    }

    /**
     * Dispatched by session layer when a Link attach
     * frame is received from the AMQP peer.
     */
    @Override
    public void attachReceived(CAMQPControlAttach data)
    {
        linkStateActor.attachReceived(data);
    }

    /**
     * Dispatched by session layer when a Link detach
     * frame is received from the AMQP peer.
     */
    @Override
    public void detachReceived(CAMQPControlDetach data)
    {
        linkStateActor.detachReceived(data); 
    }
    
    public abstract void attached();
    public abstract void detached();
    public abstract CAMQPSessionInterface getSession();
  
    /**
     * Process an incoming Link attach frame.
     * 
     * For link establishment initiator, nothing needs do be done.
     * 
     * Otherwise, set the initial delivery count
     * and source or target address from the incoming attach frame.
     * Send back an attach frame,
     * 
     * @param data
     * @param isInitiator
     */
    void processAttachReceived(CAMQPControlAttach data, boolean isInitiator)
    {
        if (isInitiator)
            return;
        
        linkHandle = CAMQPLinkManager.getNextLinkHandle();
        
        linkName = data.getName();
        
        if (role == CAMQPLinkConstants.ROLE_RECEIVER)
        {
            if (data.isSetInitialDeliveryCount())
                deliveryCount = data.getInitialDeliveryCount();
        }
        
        CAMQPControlAttach responseData = new CAMQPControlAttach();
        responseData.setHandle(linkHandle);
        responseData.setName(linkName);
        responseData.setRole(role);
        
        if (data.getSource() != null)
        {
            CAMQPDefinitionSource inSource = (CAMQPDefinitionSource) data.getSource();
            sourceAddress = (String) inSource.getAddress();
        }
        
        if (data.getTarget() != null)
        {
            CAMQPDefinitionTarget inTarget = (CAMQPDefinitionTarget) data.getTarget();
            targetAddress = (String) inTarget.getAddress();
        }
        
        CAMQPDefinitionSource source = new CAMQPDefinitionSource();
        source.setAddress(sourceAddress);
        responseData.setSource(source);
        
        CAMQPDefinitionTarget target = new CAMQPDefinitionTarget();
        target.setAddress(targetAddress);
        responseData.setTarget(target);
     
        source.setDynamic(false);
        responseData.setInitialDeliveryCount(deliveryCount);
        responseData.setMaxMessageSize(BigInteger.valueOf(CAMQPLinkConstants.DEFAULT_MAX_MESSAGE_SIZE));
        
        linkStateActor.sendAttach(responseData);       
    }
 
    /**
     * Process an incoming Link detach frame.
     * 
     * For link establishment initiator, nothing needs do be done.
     * Otherwise, send back an attach frame.
     * 
     * @param data
     * @param isInitiator
     */
    void processDetachReceived(CAMQPControlDetach data, boolean isInitiator)
    {
        if (isInitiator)
            return;
        
        CAMQPControlDetach responseData = new CAMQPControlDetach();
        responseData.setClosed(true);
        responseData.setHandle(linkHandle);
        linkStateActor.sendDetach(responseData); 
    }
    
    @GuardedBy("this")
    CAMQPControlFlow populateFlowFrame()
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
        flow.setAvailable(available);
        flow.setDeliveryCount(deliveryCount);
        flow.setLinkCredit(linkCredit);
        return flow;
    }
    
    @GuardedBy("this")
    CAMQPControlFlow populateFlowFrameAvailableUnknown()
    {
        CAMQPControlFlow flow = new CAMQPControlFlow();
        flow.setHandle(linkHandle);
        flow.setDeliveryCount(deliveryCount);
        flow.setLinkCredit(linkCredit);
        return flow;
    }
}
