
package com.ib.client;

public class TickType {
    // constants - tick types
    public static final int BID_SIZE   = 0;
    public static final int BID        = 1;
    public static final int ASK        = 2;
    public static final int ASK_SIZE   = 3;
    public static final int LAST       = 4;
    public static final int LAST_SIZE  = 5;
    public static final int HIGH       = 6;
    public static final int LOW        = 7;
    public static final int VOLUME     = 8;
    public static final int CLOSE      = 9;
    public static final int BID_OPTION = 10;
    public static final int ASK_OPTION = 11;
    public static final int LAST_OPTION = 12;
    
    public static String getField( int tickType) {
        switch( tickType) {
            case BID_SIZE:  return "bidSize";
            case BID:       return "bidPrice";
            case ASK:       return "askPrice";
            case ASK_SIZE:  return "askSize";
            case LAST:      return "lastPrice";
            case LAST_SIZE: return "lastSize";
            case HIGH:      return "high";
            case LOW:       return "low";
            case VOLUME:    return "volume";
            case CLOSE:     return "close";
            case BID_OPTION:  return "bidOptComp";
            case ASK_OPTION:  return "askOptComp";
            case LAST_OPTION: return "lastOptComp";
            default:        return "unknown";
        }
    }
}
