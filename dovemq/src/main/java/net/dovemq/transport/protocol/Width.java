package net.dovemq.transport.protocol;

public enum Width
{
    FIXED_ZERO (0),
    FIXED_ONE (1),
    FIXED_TWO (2),
    FIXED_FOUR (4),
    FIXED_EIGHT (8),
    FIXED_SIXTEEN (16),    
    VARIABLE_ONE (1),
    VARIABLE_TWO (2),
    VARIABLE_FOUR (4),
    COMPOUND_ONE (1),
    COMPOUND_TWO (2),
    COMPOUND_FOUR (4),    
    ARRAY (4);
    
    private final int widthOctets;

    Width(int widthOctets)
    {
        this.widthOctets = widthOctets;
    }
    
    protected int widthOctets()
    {
        return widthOctets;
    }
}
