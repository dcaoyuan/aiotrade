
package com.ib.client;

public class Order {
    final public static int 	CUSTOMER = 0;
    final public static int 	FIRM = 1;
    final public static char    OPT_UNKNOWN='?';
    final public static char    OPT_BROKER_DEALER='b';
    final public static char    OPT_CUSTOMER ='c';
    final public static char    OPT_FIRM='f';
    final public static char    OPT_ISEMM='m';
    final public static char    OPT_FARMM='n';
    final public static char    OPT_SPECIALIST='y';
    final public static int 	AUCTION_MATCH = 1;
    final public static int 	AUCTION_IMPROVEMENT = 2;
    final public static int 	AUCTION_TRANSPARENT = 3;
    final public static String  EMPTY_STR = "";

    // main order fields
    public int 		m_orderId;
    public int 		m_clientId;
    public int  	m_permId;
    public String 	m_action;
    public int 		m_totalQuantity;
    public String 	m_orderType;
    public double 	m_lmtPrice;
    public double 	m_auxPrice;
    public String	m_sharesAllocation;

    // extended order fields
    public String 	m_tif;
    public String 	m_ocaGroup;
    public String 	m_account;
    public String 	m_openClose;
    public int 		m_origin;
    public String 	m_orderRef;
    public boolean 	m_transmit;	// if false, order will be created but not transmited
    public int 		m_parentId;	// Parent order Id, to associate Auto STP or TRAIL orders with the original order.
    public boolean 	m_blockOrder;
    public boolean	m_sweepToFill;
    public int 		m_displaySize;
    public int 		m_triggerMethod;
    public boolean 	m_ignoreRth;
    public boolean  m_hidden;
    public double   m_discretionaryAmt;
    public String   m_goodAfterTime;
    public String   m_goodTillDate;

    public String   m_faGroup;
    public String   m_faProfile;
    public String   m_faMethod;
    public String   m_faPercentage;
    public String   m_primaryExch;
    public int      m_shortSaleSlot;      // 1 or 2.
    public String   m_designatedLocation; // when slot=2 only.

    public int      m_ocaType;  // 1 = CANCEL_WITH_BLOCK, 2 = REDUCE_WITH_BLOCK, 3 = REDUCE_NON_BLOCK
    public boolean  m_rthOnly;
    public boolean  m_overridePercentageConstraints;
    public String   m_rule80A;
    public String   m_settlingFirm;
    public boolean  m_allOrNone;
    public int      m_minQty;
    public double   m_percentOffset;
    public boolean  m_eTradeOnly;
    public boolean  m_firmQuoteOnly;
    public double   m_nbboPriceCap;

    // BOX ORDERS ONLY
    public int      m_auctionStrategy; // AUCTION_MATCH, AUCTION_IMPROVEMENT, AUCTION_TRANSPARENT
    public double   m_startingPrice;
    public double   m_stockRefPrice;
    public double   m_delta;
    public double   m_stockRangeLower;
    public double   m_stockRangeUpper;

    // VOLATILITY ORDERS ONLY
    public double   m_volatility;
    public int      m_volatilityType;     // 1=daily, 2=annual
    public int      m_continuousUpdate;
    public int      m_referencePriceType; // 1=Average, 2 = BidOrAsk
    public String   m_deltaNeutralOrderType;
    public double   m_deltaNeutralAuxPrice;

    public Order() {
        m_openClose	= "O";
        m_origin = CUSTOMER;
        m_transmit = true;
        m_primaryExch = EMPTY_STR;
        m_designatedLocation = EMPTY_STR;
        m_minQty = Integer.MAX_VALUE;
        m_percentOffset = Double.MAX_VALUE;
        m_nbboPriceCap = Double.MAX_VALUE;
        m_startingPrice = Double.MAX_VALUE;
        m_stockRefPrice = Double.MAX_VALUE;
        m_delta = Double.MAX_VALUE;
        m_stockRangeLower = Double.MAX_VALUE;
        m_stockRangeUpper = Double.MAX_VALUE;
        m_volatility = Double.MAX_VALUE;
        m_volatilityType = Integer.MAX_VALUE;
        m_deltaNeutralOrderType = EMPTY_STR;
        m_deltaNeutralAuxPrice = Double.MAX_VALUE;
        m_referencePriceType = Integer.MAX_VALUE;
    }

    public boolean equals(Object p_other) {
        if ( this == p_other )
            return true;
        else if ( p_other == null )
            return false;

        Order l_theOther = (Order)p_other;

        if ( m_permId == l_theOther.m_permId ) {
            return true;
        }

        boolean firstSetEquals = m_orderId == l_theOther.m_orderId
				&& m_clientId == l_theOther.m_clientId
				&& m_totalQuantity == l_theOther.m_totalQuantity
				&& m_lmtPrice == l_theOther.m_lmtPrice
				&& m_auxPrice == l_theOther.m_auxPrice
				&& m_origin == l_theOther.m_origin
				&& m_transmit == l_theOther.m_transmit
				&& m_parentId == l_theOther.m_parentId
				&& m_blockOrder == l_theOther.m_blockOrder
				&& m_sweepToFill == l_theOther.m_sweepToFill
				&& m_displaySize == l_theOther.m_displaySize
				&& m_triggerMethod == l_theOther.m_triggerMethod
				&& m_ignoreRth == l_theOther.m_ignoreRth
				&& m_hidden == l_theOther.m_hidden
				&& m_discretionaryAmt == l_theOther.m_discretionaryAmt
				&& m_shortSaleSlot == l_theOther.m_shortSaleSlot
				&& m_designatedLocation == l_theOther.m_designatedLocation
				&& m_ocaType == l_theOther.m_ocaType
				&& m_rthOnly == l_theOther.m_rthOnly
				&& m_allOrNone == l_theOther.m_allOrNone
				&& m_minQty == l_theOther.m_minQty
				&& m_percentOffset == l_theOther.m_percentOffset
				&& m_eTradeOnly == l_theOther.m_eTradeOnly
				&& m_firmQuoteOnly == l_theOther.m_firmQuoteOnly
				&& m_nbboPriceCap == l_theOther.m_nbboPriceCap
				&& m_auctionStrategy == l_theOther.m_auctionStrategy
				&& m_startingPrice == l_theOther.m_startingPrice
				&& m_stockRefPrice == l_theOther.m_stockRefPrice
				&& m_delta == l_theOther.m_delta
				&& m_stockRangeLower == l_theOther.m_stockRangeLower
				&& m_stockRangeUpper == l_theOther.m_stockRangeUpper
				&& m_volatility == l_theOther.m_volatility
				&& m_volatilityType == l_theOther.m_volatilityType
				&& m_deltaNeutralAuxPrice == l_theOther.m_deltaNeutralAuxPrice
				&& m_continuousUpdate == l_theOther.m_continuousUpdate
				&& m_referencePriceType == l_theOther.m_referencePriceType;

        if (!firstSetEquals) {
        	return false;
        } else {
            String l_thisAction = m_action != null ? m_action : EMPTY_STR;
            String l_thisOrderType  = m_orderType != null ? m_orderType : EMPTY_STR;
            String l_thisTif = m_tif != null ? m_tif : EMPTY_STR;
            String l_thisOcaGroup = m_ocaGroup != null ? m_ocaGroup : EMPTY_STR;
            String l_thisAccount = m_account != null ? m_account : EMPTY_STR;
            String l_thisOpenClose = m_openClose != null ? m_openClose : EMPTY_STR;
            String l_thisOrderRef = m_orderRef != null ? m_orderRef : EMPTY_STR;
            String l_thisPrimaryExch = m_primaryExch != null ? m_primaryExch : EMPTY_STR ;
            String l_thisRule80A = m_rule80A != null ? m_rule80A : EMPTY_STR ;
            String l_thisSettlingFirm = m_settlingFirm != null ? m_settlingFirm : EMPTY_STR;
            String l_thisDeltaNeutralOrderType = m_deltaNeutralOrderType != null ? m_deltaNeutralOrderType : EMPTY_STR;
            
            String l_otherAction = l_theOther.m_action != null ? l_theOther.m_action : EMPTY_STR;
            String l_otherOrderType  = l_theOther.m_orderType != null ? l_theOther.m_orderType : EMPTY_STR;
            String l_otherTif = l_theOther.m_tif != null ? l_theOther.m_tif : EMPTY_STR;
            String l_otherOcaGroup = l_theOther.m_ocaGroup != null ? l_theOther.m_ocaGroup : EMPTY_STR;
            String l_otherAccount = l_theOther.m_account != null ? l_theOther.m_account : EMPTY_STR;
            String l_otherOpenClose = l_theOther.m_openClose != null ? l_theOther.m_openClose : EMPTY_STR;
            String l_otherOrderRef = l_theOther.m_orderRef != null ? l_theOther.m_orderRef : EMPTY_STR;
            String l_otherOrderGoodAfterTime = l_theOther.m_goodAfterTime != null ? l_theOther.m_goodAfterTime : EMPTY_STR;
            String l_otherOrderGoodTillDate = l_theOther.m_goodTillDate != null ? l_theOther.m_goodTillDate : EMPTY_STR;
            String l_otherPrimaryExch = l_theOther.m_primaryExch != null? l_theOther.m_primaryExch : EMPTY_STR ;
            String l_otherRule80A = l_theOther.m_rule80A != null? l_theOther.m_rule80A : EMPTY_STR ;
            String l_otherSettlingFirm = l_theOther.m_settlingFirm != null ? l_theOther.m_settlingFirm : EMPTY_STR;
            String l_otherDeltaNeutralOrderType = l_theOther.m_deltaNeutralOrderType != null ? l_theOther.m_deltaNeutralOrderType : EMPTY_STR;
        
        return  
        	l_thisAction.equals(l_otherAction)
					&& l_thisOrderType.equals(l_otherOrderType)
					&& l_thisTif.equals(l_otherTif)
					&& l_thisOcaGroup.equals(l_otherOcaGroup)
					&& l_thisAccount.equals(l_otherAccount)
					&& l_thisOpenClose.equals(l_otherOpenClose)
					&& l_thisOrderRef.equals(l_otherOrderRef)
					&& l_otherOrderGoodAfterTime.equals(l_otherOrderGoodAfterTime)
					&& l_otherOrderGoodTillDate.equals(l_otherOrderGoodTillDate)
					&& l_thisPrimaryExch.equals(l_otherPrimaryExch)
					&& l_thisRule80A.equals(l_otherRule80A)
					&& l_thisSettlingFirm.equals(l_otherSettlingFirm)
					&& l_thisDeltaNeutralOrderType.equals(l_otherDeltaNeutralOrderType);
        }
    }
}
