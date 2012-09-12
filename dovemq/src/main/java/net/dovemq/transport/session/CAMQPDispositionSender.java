package net.dovemq.transport.session;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlDisposition;
import net.dovemq.transport.protocol.data.CAMQPDefinitionAccepted;
import net.dovemq.transport.protocol.data.CAMQPDefinitionDeliveryState;
import net.dovemq.transport.protocol.data.CAMQPDefinitionModified;
import net.dovemq.transport.protocol.data.CAMQPDefinitionRejected;
import net.dovemq.transport.session.CAMQPSession.CAMQPChannel;

import org.jboss.netty.buffer.ChannelBuffer;

class CAMQPDispositionSender implements Runnable
{
    static class DispositionRange
    {
        @Override
        public String toString()
        {
            String outcomeStr = "null";
            if (outcome instanceof CAMQPDefinitionAccepted)
                outcomeStr = CAMQPDefinitionAccepted.class.getSimpleName();
            else if (outcome instanceof CAMQPDefinitionModified)
                outcomeStr = CAMQPDefinitionModified.class.getSimpleName();
            else if (outcome instanceof CAMQPDefinitionRejected)
                outcomeStr =CAMQPDefinitionRejected.class.getSimpleName();
            
            return "DispositionRange [min=" + min
                    + ", max="
                    + max
                    + ", settled="
                    + settled
                    + ", outcome="
                    + outcomeStr
                    + "]";
        }

        public DispositionRange(long min,
                long max,
                boolean settled,
                Object state)
        {
            super();
            this.min = min;
            this.max = max;
            this.settled = settled;
            this.outcome = state;
        }

        public boolean isSettled()
        {
            return settled;
        }

        public void setSettled(boolean settled)
        {
            this.settled = settled;
        }

        public Object getOutcome()
        {
            return outcome;
        }

        public void setOutcome(Object outcome)
        {
            this.outcome = outcome;
        }

        public void setMin(long min)
        {
            this.min = min;
        }

        public void setMax(long max)
        {
            this.max = max;
        }

        public long getMin()
        {
            return min;
        }

        public long getMax()
        {
            return max;
        }
        
        boolean isCompatible(boolean settled, Object newOutcome)
        {
            if (this.settled != settled)
                return false;

            if (outcome == newOutcome)
                return true;
            
            if ((outcome instanceof CAMQPDefinitionAccepted) && (newOutcome instanceof CAMQPDefinitionAccepted))
                return true;
            if ((outcome instanceof CAMQPDefinitionModified) && (newOutcome instanceof CAMQPDefinitionModified))
                return true;
            if ((outcome instanceof CAMQPDefinitionRejected) && (newOutcome instanceof CAMQPDefinitionRejected))
                return true;          
            
            return false;
        }

        private long min;
        private long max;
        private boolean settled;
        private Object outcome;
    }

    private List<DispositionRange> senderDispositionRanges = null;
    private List<DispositionRange> receiverDispositionRanges = null;    
    private final CAMQPSession session;

    CAMQPDispositionSender(CAMQPSession session)
    {
        super();
        this.session = session;
    }
    
    synchronized void insertDispositionRange(long val, boolean role, boolean settled, Object newOutcome)
    {
        List<DispositionRange> dispositionRanges;
        if (role)
        {
            if (senderDispositionRanges == null)
            {
                senderDispositionRanges = new LinkedList<DispositionRange>();
            }
            dispositionRanges = senderDispositionRanges;
        }
        else
        {
            if (receiverDispositionRanges == null)
            {
                receiverDispositionRanges = new LinkedList<DispositionRange>();
            }
            dispositionRanges = receiverDispositionRanges;           
        }
        addDisposition(val, settled, newOutcome, dispositionRanges);
    }

    @Override
    public void run()
    {
        List<DispositionRange> localSenderDispositionRanges = null;
        List<DispositionRange> localReceiverDispositionRanges = null;        
        synchronized (this)
        {
            localSenderDispositionRanges = senderDispositionRanges;
            localReceiverDispositionRanges = receiverDispositionRanges;
            senderDispositionRanges = null;
            receiverDispositionRanges = null;
        }

        CAMQPChannel channel = session.getChannel();
        if (channel == null)
        {
            return;
        }

        try
        {
            sendDispositions(localSenderDispositionRanges, true, channel);
            sendDispositions(localReceiverDispositionRanges, false, channel);            
        }
        finally
        {
            session.flowSendScheduler.schedule(this, CAMQPSessionConstants.BATCHED_DISPOSITION_SEND_INTERVAL, TimeUnit.MILLISECONDS); 
        }
    }
    
    private static void sendDispositions(List<DispositionRange> dispositionRanges, boolean role, CAMQPChannel channel)
    {
        if (dispositionRanges != null)
        {
            for (DispositionRange range : dispositionRanges)
            {
                CAMQPControlDisposition disposition = new CAMQPControlDisposition();
                disposition.setBatchable(false);
                disposition.setFirst(range.getMin());
                disposition.setLast(range.getMax());
                disposition.setRole(role);
                disposition.setSettled(range.isSettled());
                if (range.getOutcome() != null)
                {
                    CAMQPDefinitionDeliveryState deliveryState = new CAMQPDefinitionDeliveryState();
                    deliveryState.setOutcome(range.getOutcome());
                    disposition.setState(deliveryState);
                }
                CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
                CAMQPControlDisposition.encode(encoder, disposition);
                ChannelBuffer encodedTransfer = encoder.getEncodedBuffer();
                channel.getAmqpConnection().sendFrame(encodedTransfer, channel.getChannelId());                    
            }
        }        
    }
    
    static void addDisposition(long val, boolean settled, Object newOutcome, List<DispositionRange> dispositionRanges)
    {
        if (dispositionRanges.isEmpty())
        {
            dispositionRanges.add(new DispositionRange(val, val, settled, newOutcome));
            return;
        }
        
        if (isUpdate(val, settled, newOutcome, dispositionRanges))
            return;

        Iterator<DispositionRange> iter = dispositionRanges.iterator();
        DispositionRange range = iter.next();
        DispositionRange nextRange = null;

        int indexToInsert = 0;
        while (range != null)
        {
            if (val < range.getMin()-1)
            {
                dispositionRanges.add(indexToInsert, new DispositionRange(val, val, settled, newOutcome));
                return;
            }

            if (iter.hasNext()) {
                nextRange = iter.next();
            }
 
            if (nextRange != null)
            {
                if (val == range.getMax() + 1 && val == nextRange.getMin() - 1)
                {
                    if (range.isCompatible(settled, newOutcome) && nextRange.isCompatible(settled, newOutcome))
                    {
                        range.setMax(nextRange.getMax());
                        dispositionRanges.remove(nextRange);
                        return;
                    }
                    else if (nextRange.isCompatible(settled, newOutcome))
                    {
                        nextRange.setMin(val);
                        return;
                    }
                    else if (range.isCompatible(settled, newOutcome))
                    {
                        range.setMax(val);
                        return;
                    }
                    else
                    {
                        dispositionRanges.add(indexToInsert+1, new DispositionRange(val, val, settled, newOutcome));
                        return;
                    }
                }
            }
                
            if (val == range.getMax()+1)
            {
                if (range.isCompatible(settled, newOutcome))
                {
                    range.setMax(val);
                    return;
                }
                else
                {
                    dispositionRanges.add(indexToInsert+1, new DispositionRange(val, val, settled, newOutcome));
                    return;
                }
            }
            if (val == range.getMin()-1)
            {
                if (range.isCompatible(settled, newOutcome))
                {
                    range.setMin(val);
                    return;
                }
                else
                {
                    dispositionRanges.add(indexToInsert, new DispositionRange(val, val, settled, newOutcome));
                    return;
                }
            }

            if (val > range.getMax()+1)
            {
                range = nextRange;
                nextRange = null;
                indexToInsert++;
                continue;
            }
            
            assert(false);
        }
        dispositionRanges.add(new DispositionRange(val, val, settled, newOutcome));
    }
    
    private static boolean isUpdate(long val, boolean settled, Object newOutcome, List<DispositionRange> dispositionRanges)
    {
        Iterator<DispositionRange> iter = dispositionRanges.iterator();
        DispositionRange range = iter.next();
        DispositionRange nextRange = null;
        DispositionRange prevRange = null;
        boolean updated = false;
        
        int index = 0;
        while (range != null)
        {
            if (val < range.getMin())
                return false;
            
            if (iter.hasNext()) {
                nextRange = iter.next();
            }
            
            if (val>=range.getMin() && val<=range.getMax())
            {
                if (range.isCompatible(settled, newOutcome))
                {
                    return true;
                }
                updated = true;
                break;
            }
            prevRange = range;
            range = nextRange;
            nextRange = null;
            index++;
        }

        if (!updated)
            return false;
        
        if (range.getMin()==range.getMax() && val==range.getMax())
        {
            range.setSettled(settled);
            range.setOutcome(newOutcome);
            
            if (prevRange!=null && nextRange!= null)
            {
                if (prevRange.getMax()+1 == nextRange.getMin()-1)
                {
                    if (prevRange.isCompatible(settled, newOutcome) && nextRange.isCompatible(settled, newOutcome))
                    {
                        prevRange.setMax(nextRange.getMax());
                        dispositionRanges.remove(range);
                        dispositionRanges.remove(nextRange);
                        return true;
                    }
                }
            }
            if (prevRange!=null && (prevRange.getMax()+1 == val))
            {
                if (prevRange.isCompatible(settled, newOutcome))
                {
                    prevRange.setMax(val);
                    dispositionRanges.remove(range);
                    return true;
                }
            }
            if (nextRange!=null && (nextRange.getMin()-1 == val))
            {
                if (nextRange.isCompatible(settled, newOutcome))
                {
                    nextRange.setMin(val);
                    dispositionRanges.remove(range);
                    return true;
                }
            }
            return true;
        }
        
        DispositionRange newRange = new DispositionRange(val, val, settled, newOutcome);
        
        if (range.getMin() == val)
        {
            range.setMin(range.getMin() + 1);
            
            if (prevRange!=null && (prevRange.getMax()+1 == val))
            {
                if (prevRange.isCompatible(settled, newOutcome))
                {
                    prevRange.setMax(val);
                    return true;
                }
            }
            dispositionRanges.add(index, newRange);
            return true;
        }
        else if (range.getMax() == val)
        {
            range.setMax(range.getMax() - 1);
            
            if (nextRange!=null && (nextRange.getMin()-1 == val))
            {
                if (nextRange.isCompatible(settled, newOutcome))
                {
                    nextRange.setMin(val);
                    return true;
                }
            }

            if (nextRange != null)
                dispositionRanges.add(index+1, newRange);
            else
                dispositionRanges.add(newRange);
            return true;
        }
        else if (val>range.getMin() && val <range.getMax())
        {
            DispositionRange splitRange = new DispositionRange(range.getMin(), val-1, range.isSettled(), range.getOutcome());
            range.setMin(val + 1);
            
            dispositionRanges.add(index, newRange);
            dispositionRanges.add(index, splitRange);
            return true;
        }
      
        return true;
    }
}
