package net.dovemq.transport.link;

import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPDefinitionSource;
import net.dovemq.transport.protocol.data.CAMQPDefinitionTarget;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

class CAMQPLinkKey
{
    private final String source;
    private final String target;
    
    public CAMQPLinkKey(String source, String target)
    {
        super();
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ((obj == null) || (!(obj instanceof CAMQPLinkKey)))
            return false;       
        
        CAMQPLinkKey otherKey = (CAMQPLinkKey) obj;
        
        return (StringUtils.equalsIgnoreCase(this.source, otherKey.source) &&
                StringUtils.equalsIgnoreCase(this.target, otherKey.target));
    }
    
    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    
    @Override
    public String toString()
    {
        return source + ":" + target;
    }
    
    public static CAMQPLinkKey createLinkKey(CAMQPControlAttach attach)
    {
        String sourceString = null;
        String targetString = null;        
        Object source = attach.getSource();
        if (source instanceof CAMQPDefinitionSource)
        {
            CAMQPDefinitionSource endpoint = (CAMQPDefinitionSource) source;
            sourceString = (String) endpoint.getAddress();
        }
        
        Object target = attach.getTarget();
        if (target instanceof CAMQPDefinitionTarget)
        {
            CAMQPDefinitionTarget endpoint = (CAMQPDefinitionTarget) target;
            targetString = (String) endpoint.getAddress();
        }
        
        if (sourceString!=null && targetString!=null)
        {
            return new CAMQPLinkKey(sourceString, targetString);
        }
        else
        {
            return null;
        }   
    }
}
