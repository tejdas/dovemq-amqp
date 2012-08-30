package net.dovemq.transport.link;

import java.math.BigInteger;
import java.util.UUID;

import org.apache.log4j.Logger;

import net.jcip.annotations.GuardedBy;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPDefinitionError;
import net.dovemq.transport.protocol.data.CAMQPDefinitionSource;
import net.dovemq.transport.protocol.data.CAMQPDefinitionTarget;
import net.dovemq.transport.session.CAMQPSessionInterface;

enum LinkRole
{
    LinkSender,
    LinkReceiver
}

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
    private static final Logger log = Logger.getLogger(CAMQPLinkEndpoint.class);
    
    abstract LinkRole getRole();
    
    private final String roleAsString;
    private String linkName;
    private final CAMQPLinkStateActor linkStateActor;
    
    protected long linkHandle;
    
    private String sourceAddress;
    private String targetAddress;
    private CAMQPLinkKey linkKey;
    
    public CAMQPLinkKey getLinkKey()
    {
        return linkKey;
    }

    /*
     * Flow-control attributes
     */
    protected long deliveryCount = 0;
    protected long linkCredit = 0;
    protected long available = 0;

    public CAMQPLinkEndpoint()
    {
        super();
        this.roleAsString = (getRole() == LinkRole.LinkSender)? "LinkSender" : "LinkReceiver";
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
        CAMQPLinkManager.getLinkHandshakeTracker().registerOutstandingLink(linkName, this);
        
        CAMQPControlAttach data = new CAMQPControlAttach();
        data.setHandle(linkHandle);
        data.setName(linkName);
        data.setRole(getRole() == LinkRole.LinkReceiver);
        
        CAMQPDefinitionSource source = new CAMQPDefinitionSource();
        source.setAddress(sourceAddress);
        data.setSource(source);
        
        CAMQPDefinitionTarget target = new CAMQPDefinitionTarget();
        target.setAddress(targetAddress);
        data.setTarget(target);
     
        source.setDynamic(false);
        if (getRole() == LinkRole.LinkSender)
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
        linkKey = CAMQPLinkKey.createLinkKey(data);
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
    
    public void attached(boolean isInitiator)
    {
        if (linkKey != null)
        {
            CAMQPLinkManager.getLinkmanager().registerLinkEndpoint(linkName, linkKey, this);
        }
        String initiatedBy =  isInitiator? "self" : "peer";
        log.debug(roleAsString + " created between source: " + sourceAddress + " and target: " + targetAddress + " . Initiated by: " + initiatedBy);
    }

    public void detached(boolean isInitiator)
    {
        if (linkKey != null)
        {
            CAMQPLinkManager.getLinkmanager().unregisterLinkEndpoint(linkName, linkKey);
        }
        String initiatedBy =  isInitiator? "self" : "peer";
        log.debug(roleAsString + " destroyed between source: " + sourceAddress + " and target: " + targetAddress + " . Initiated by: " + initiatedBy);
    }

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
        
        if (getRole() == LinkRole.LinkReceiver)
        {
            if (data.isSetInitialDeliveryCount())
                deliveryCount = data.getInitialDeliveryCount();
        }
        
        CAMQPControlAttach responseData = new CAMQPControlAttach();
        responseData.setHandle(linkHandle);
        responseData.setName(linkName);
        responseData.setRole(getRole() == LinkRole.LinkReceiver);
        
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
