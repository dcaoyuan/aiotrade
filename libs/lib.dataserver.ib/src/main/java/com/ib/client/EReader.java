
package com.ib.client;

import java.io.*;
import java.net.*;

class EReader extends Thread {

    // incoming msg id's
    static final int TICK_PRICE		= 1;
    static final int TICK_SIZE		= 2;
    static final int ORDER_STATUS	= 3;
    static final int ERR_MSG		= 4;
    static final int OPEN_ORDER         = 5;
    static final int ACCT_VALUE         = 6;
    static final int PORTFOLIO_VALUE    = 7;
    static final int ACCT_UPDATE_TIME   = 8;
    static final int NEXT_VALID_ID      = 9;
    static final int CONTRACT_DATA      = 10;
    static final int EXECUTION_DATA     = 11;
    static final int MARKET_DEPTH     	= 12;
    static final int MARKET_DEPTH_L2    = 13;
    static final int NEWS_BULLETINS    	= 14;
    static final int MANAGED_ACCTS    	= 15;
    static final int RECEIVE_FA    	= 16;
    static final int HISTORICAL_DATA    = 17;
    static final int BOND_CONTRACT_DATA = 18;
    static final int SCANNER_PARAMETERS = 19;
    static final int SCANNER_DATA       = 20;
    static final int TICK_OPTION_COMPUTATION   = 21;

    private EClientSocket 	m_parent;
    private DataInputStream m_dis;
    private EWrapper 		m_eWrapper;
    
    EReader( EClientSocket parent, DataInputStream dis) {
        setName( "EReader");
        m_parent = parent;
        m_dis = dis;
        m_eWrapper = parent.eWrapper();
    }

    public void run() {
        try {
            processMsgs();
        }
        catch( Exception e) {
            m_parent.error( "Error reading - " + e);
        }
        m_parent.close();
    }

    void processMsgs() throws IOException {
        try {
            // loop until thread is terminated
            while( !isInterrupted() ) {
                int msgId = readInt();
                if( msgId == -1) return;
                switch( msgId) {
                    case TICK_PRICE: {
                        int version = readInt();
                        int tickerId = readInt();
                        int tickType = readInt();
                        double price = readDouble();
                        int size = 0;
                        if( version >= 2) {
                            size = readInt();
                        }
                        int canAutoExecute = 0;
                        if (version >= 3) {
                            canAutoExecute = readInt();
                        }
                        m_eWrapper.tickPrice( tickerId, tickType, price, canAutoExecute);

                        if( version >= 2) {
                            int sizeTickType = -1 ; // not a tick
                            switch (tickType) {
                                case 1: // BID
                                    sizeTickType = 0 ; // BID_SIZE
                                    break ;
                                case 2: // ASK
                                    sizeTickType = 3 ; // ASK_SIZE
                                    break ;
                                case 4: // LAST
                                    sizeTickType = 5 ; // LAST_SIZE
                                    break ;
                            }
                            if (sizeTickType != -1)
                                m_eWrapper.tickSize( tickerId, sizeTickType, size);
                        }
                        break;
                    }
                    
                    case TICK_SIZE: {
                        int version = readInt();
                        int tickerId = readInt();
                        int tickType = readInt();
                        int size = readInt();

                        m_eWrapper.tickSize( tickerId, tickType, size);
                        break;
                    }

                    case TICK_OPTION_COMPUTATION: {
                        int version = readInt();
                        int tickerId = readInt();
                        int tickType = readInt();
                        double impliedVol = readDouble();
                    	if (impliedVol < 0) { // -1 is the "not yet computed" indicator
                    		impliedVol = Double.MAX_VALUE;
                    	}
                        double delta = readDouble();
                    	if (Math.abs(delta) > 1) { // -2 is the "not yet computed" indicator
                    		delta = Double.MAX_VALUE;
                    	}
                        m_eWrapper.tickOptionComputation( tickerId, tickType, impliedVol, delta);
                    	break;
                    }
                    	
                    case ORDER_STATUS: {
                        int version = readInt();
                        int id = readInt();
                        String status = readStr();
                        int filled = readInt();
                        int remaining = readInt();
                        double avgFillPrice = readDouble();

                        int permId = 0;
                        if( version >= 2) {
                            permId = readInt();
                        }

                        int parentId = 0;
                        if( version >= 3) {
                            parentId = readInt();
                        }

                        double lastFillPrice = 0;
                        if( version >= 4) {
                            lastFillPrice = readDouble();
                        }

                        int clientId = 0;
                        if( version >= 5) {
                            clientId = readInt();
                        }

                        m_eWrapper.orderStatus( id, status, filled, remaining, avgFillPrice,
                                        permId, parentId, lastFillPrice, clientId);
                        break;
                    }

                    case ACCT_VALUE: {
                        int version = readInt();
                        String key = readStr();
                        String val  = readStr();
                        String cur = readStr();
                        String accountName = null ;
                        if( version >= 2) {
                            accountName = readStr();
                        }
                        m_eWrapper.updateAccountValue(key, val, cur, accountName);
                        break;
                    }

                    case PORTFOLIO_VALUE: {
                        int version = readInt();
                        Contract contract = new Contract();
                        contract.m_symbol  = readStr();
                        contract.m_secType = readStr();
                        contract.m_expiry  = readStr();
                        contract.m_strike  = readDouble();
                        contract.m_right   = readStr();
                        contract.m_currency = readStr();
                        if ( version >= 2 ) {
                            contract.m_localSymbol = readStr();
                        }

                        int position  = readInt();
                        double marketPrice = readDouble();
                        double marketValue = readDouble();
                        double  averageCost = 0.0;
                        double  unrealizedPNL = 0.0;
                        double  realizedPNL = 0.0;
                        if (version >=3 ) {
                            averageCost = readDouble();
                            unrealizedPNL = readDouble();
                            realizedPNL = readDouble();
                        }

                        String accountName = null ;
                        if( version >= 4) {
                            accountName = readStr();
                        }

                        m_eWrapper.updatePortfolio(contract, position, marketPrice, marketValue,
                                        averageCost, unrealizedPNL, realizedPNL, accountName);

                        break;
                    }

                    case ACCT_UPDATE_TIME: {
                        int version = readInt();
                        String timeStamp = readStr();
                        m_eWrapper.updateAccountTime(timeStamp);
                        break;
                    }

                    case ERR_MSG: {
                        int version = readInt();
                        if(version < 2) {
                            String msg = readStr();
                            m_parent.error( msg);
                        } else {
                            int id = readInt();
                            int errorCode    = readInt();
                            String errorMsg = readStr();
                            m_parent.error(id, errorCode, errorMsg);
                        }
                        break;
                    }

                    case OPEN_ORDER: {
                        // read version
                        int version = readInt();

                        // read order id
                        Order order = new Order();
                        order.m_orderId = readInt();

                        // read contract fields
                        Contract contract = new Contract();
                        contract.m_symbol = readStr();
                        contract.m_secType = readStr();
                        contract.m_expiry = readStr();
                        contract.m_strike = readDouble();
                        contract.m_right = readStr();
                        contract.m_exchange = readStr();
                        contract.m_currency = readStr();
                        if ( version >= 2 ) {
                            contract.m_localSymbol = readStr();
                        }

                        // read order fields
                        order.m_action = readStr();
                        order.m_totalQuantity = readInt();
                        order.m_orderType = readStr();
                        order.m_lmtPrice = readDouble();
                        order.m_auxPrice = readDouble();
                        order.m_tif = readStr();
                        order.m_ocaGroup = readStr();
                        order.m_account = readStr();
                        order.m_openClose = readStr();
                        order.m_origin = readInt();
                        order.m_orderRef = readStr();

                        if(version >= 3) {
                            order.m_clientId = readInt();
                        }

                        if( version >= 4 ) {
                            order.m_permId = readInt();
                            order.m_ignoreRth = readInt() == 1;
                            order.m_hidden = readInt() == 1;
                            order.m_discretionaryAmt = readDouble();
                        }

                        if ( version >= 5 ) {
                            order.m_goodAfterTime = readStr();
                        }

                        if ( version >= 6 ) {
                            order.m_sharesAllocation = readStr();
                        }

                        if ( version >= 7 ) {
                            order.m_faGroup = readStr();
                            order.m_faMethod = readStr();
                            order.m_faPercentage = readStr();
                            order.m_faProfile = readStr();
                        }

                        if ( version >= 8 ) {
                            order.m_goodTillDate = readStr();
                        }

                        if ( version >= 9) {
                            order.m_rule80A = readStr();
                            order.m_percentOffset = readDouble();
                            order.m_settlingFirm = readStr();
                            order.m_shortSaleSlot = readInt();
                            order.m_designatedLocation = readStr();
                            order.m_auctionStrategy = readInt();
                            order.m_startingPrice = readDouble();
                            order.m_stockRefPrice = readDouble();
                            order.m_delta = readDouble();
                            order.m_stockRangeLower = readDouble();
                            order.m_stockRangeUpper = readDouble();
                            order.m_displaySize = readInt();
                            order.m_rthOnly = readBoolFromInt();
                            order.m_blockOrder = readBoolFromInt();
                            order.m_sweepToFill = readBoolFromInt();
                            order.m_allOrNone = readBoolFromInt();
                            order.m_minQty = readInt();
                            order.m_ocaType = readInt();
                            order.m_eTradeOnly = readBoolFromInt();
                            order.m_firmQuoteOnly = readBoolFromInt();
                            order.m_nbboPriceCap = readDouble();
                        }

                        if ( version >= 10) {
                            order.m_parentId = readInt();
                            order.m_triggerMethod = readInt();
                        }

                        if (version >= 11) {
                            order.m_volatility = readDouble();
                            order.m_volatilityType = readInt();
                            if (version == 11) {
                            	int receivedInt = readInt();
                            	order.m_deltaNeutralOrderType = ( (receivedInt == 0) ? "NONE" : "MKT" ); 
                            } else {
                            	order.m_deltaNeutralOrderType = readStr();
                            	order.m_deltaNeutralAuxPrice = readDouble();
                            }
                            order.m_continuousUpdate = readInt();
                            if (m_parent.serverVersion() == 26) {
                            	order.m_stockRangeLower = readDouble();
                            	order.m_stockRangeUpper = readDouble();
                            }
                            order.m_referencePriceType = readInt();
                        }

                        m_eWrapper.openOrder( order.m_orderId, contract, order);
                        break;
                    }

                    case NEXT_VALID_ID: {
                        int version = readInt();
                        int orderId = readInt();
                        m_eWrapper.nextValidId( orderId);
                        break;
                    }

                    case SCANNER_DATA: {
                        ContractDetails contract = new ContractDetails();
                        int version = readInt();
                        int tickerId = readInt();
                        int numberOfElements = readInt();
                        for (int ctr=0; ctr < numberOfElements; ctr++) {
                            int rank = readInt();
                            contract.m_summary.m_symbol = readStr();
                            contract.m_summary.m_secType = readStr();
                            contract.m_summary.m_expiry = readStr();
                            contract.m_summary.m_strike = readDouble();
                            contract.m_summary.m_right = readStr();
                            contract.m_summary.m_exchange = readStr();
                            contract.m_summary.m_currency = readStr();
                            contract.m_summary.m_localSymbol = readStr();
                            contract.m_marketName = readStr();
                            contract.m_tradingClass = readStr();
                            String distance = readStr();
                            String benchmark = readStr();
                            String projection = readStr();
                            m_eWrapper.scannerData(tickerId, rank, contract, distance,
                                benchmark, projection);
                        }
                        break;
                    }

                    case CONTRACT_DATA: {
                        int version = readInt();
                        ContractDetails contract = new ContractDetails();
                        contract.m_summary.m_symbol = readStr();
                        contract.m_summary.m_secType = readStr();
                        contract.m_summary.m_expiry = readStr();
                        contract.m_summary.m_strike = readDouble();
                        contract.m_summary.m_right = readStr();
                        contract.m_summary.m_exchange = readStr();
                        contract.m_summary.m_currency = readStr();
                        contract.m_summary.m_localSymbol = readStr();
                        contract.m_marketName = readStr();
                        contract.m_tradingClass = readStr();
                        contract.m_conid = readInt();
                        contract.m_minTick = readDouble();
                        contract.m_multiplier = readStr();
                        contract.m_orderTypes = readStr();
                        contract.m_validExchanges = readStr();
                        if (version >= 2) {
                            contract.m_priceMagnifier = readInt();
                        }
                        m_eWrapper.contractDetails( contract);
                        break;
                    }
                    case BOND_CONTRACT_DATA: {
                        int version = readInt();
                        ContractDetails contract = new ContractDetails();

                        contract.m_summary.m_symbol = readStr();
                        contract.m_summary.m_secType = readStr();
                        contract.m_summary.m_cusip = readStr();
                        contract.m_summary.m_coupon = readDouble();
                        contract.m_summary.m_maturity = readStr();
                        contract.m_summary.m_issueDate  = readStr();
                        contract.m_summary.m_ratings = readStr();
                        contract.m_summary.m_bondType = readStr();
                        contract.m_summary.m_couponType = readStr();
                        contract.m_summary.m_convertible = readBoolFromInt();
                        contract.m_summary.m_callable = readBoolFromInt();
                        contract.m_summary.m_putable = readBoolFromInt();
                        contract.m_summary.m_descAppend = readStr();
                        contract.m_summary.m_exchange = readStr();
                        contract.m_summary.m_currency = readStr();
                        contract.m_marketName = readStr();
                        contract.m_tradingClass = readStr();
                        contract.m_conid = readInt();
                        contract.m_minTick = readDouble();
                        contract.m_orderTypes = readStr();
                        contract.m_validExchanges = readStr();
                        m_eWrapper.bondContractDetails( contract);
                        break;
                    }
                    case EXECUTION_DATA: {
                        int version = readInt();
                        int orderId = readInt();

                        Contract contract = new Contract();
                        contract.m_symbol = readStr();
                        contract.m_secType = readStr();
                        contract.m_expiry = readStr();
                        contract.m_strike = readDouble();
                        contract.m_right = readStr();
                        contract.m_exchange = readStr();
                        contract.m_currency = readStr();
                        contract.m_localSymbol = readStr();

                        Execution exec = new Execution();
                        exec.m_orderId = orderId;
                        exec.m_execId = readStr();
                        exec.m_time = readStr();
                        exec.m_acctNumber = readStr();
                        exec.m_exchange = readStr();
                        exec.m_side = readStr();
                        exec.m_shares = readInt();
                        exec.m_price = readDouble();
                        if ( version >= 2 ) {
                            exec.m_permId = readInt();
                        }
                        if ( version >= 3) {
                            exec.m_clientId = readInt();
                        }
                        if ( version >= 4) {
                            exec.m_liquidation = readInt();
                        }

                        m_eWrapper.execDetails( orderId, contract, exec);
                        break;
                    }
                    case MARKET_DEPTH: {
                        int version = readInt();
                        int id = readInt();

                        int position = readInt();
                        int operation = readInt();
                        int side = readInt();
                        double price = readDouble();
                        int size = readInt();

                        m_eWrapper.updateMktDepth(id, position, operation,
                                        side, price, size);
                        break;
                    }
                    case MARKET_DEPTH_L2: {
                        int version = readInt();
                        int id = readInt();

                        int position = readInt();
                        String marketMaker = readStr();
                        int operation = readInt();
                        int side = readInt();
                        double price = readDouble();
                        int size = readInt();

                        m_eWrapper.updateMktDepthL2(id, position, marketMaker,
                                        operation, side, price, size);
                        break;
                    }
                    case NEWS_BULLETINS: {
                        int version = readInt();
                        int newsMsgId = readInt();
                        int newsMsgType = readInt();
                        String newsMessage = readStr();
                        String originatingExch = readStr();

                        m_eWrapper.updateNewsBulletin( newsMsgId, newsMsgType, newsMessage, originatingExch);
                        break;
                    }
                    case MANAGED_ACCTS: {
                        int version = readInt();
                        String accountsList = readStr();

                        m_eWrapper.managedAccounts( accountsList);
                        break;
                    }
                    case RECEIVE_FA: {
                      int version = readInt();
                      int faDataType = readInt();
                      String xml = readStr();

                      m_eWrapper.receiveFA(faDataType, xml);
                      break;
                    }
                    case HISTORICAL_DATA: {
                      int version = readInt();
                      int reqId = readInt();
                	  String startDateStr;
                	  String endDateStr;
                	  String completedIndicator = "finished";
                      if (version >= 2) {
                    	  startDateStr = readStr();
                    	  endDateStr = readStr();
                    	  completedIndicator += "-" + startDateStr + "-" + endDateStr;
                      }
                      int itemCount = readInt();
                      for (int ctr = 0; ctr < itemCount; ctr++) {
                        String date = readStr();
                        double open = readDouble();
                        double high = readDouble();
                        double low = readDouble();
                        double close = readDouble();
                        double prevClose =  0;
                        int volume = readInt();
                        double WAP = readDouble();
                        String hasGaps = readStr();
                        m_eWrapper.historicalData(reqId, date, open, high, low, close,
                                                    prevClose, volume, WAP,
                                                Boolean.valueOf(hasGaps).booleanValue());
                      }
                      // send end of dataset marker
                      m_eWrapper.historicalData(reqId, completedIndicator, -1, -1, -1,-1, -1, -1, 1, false);
                      break;
                    }
                    case SCANNER_PARAMETERS: {
                        int version = readInt();
                        String xml = readStr();
                        m_eWrapper.scannerParameters(xml);
                        break;
                    }
                    default: {
                        m_parent.error( EClientErrors.NO_VALID_ID, EClientErrors.UNKNOWN_ID.code(), EClientErrors.UNKNOWN_ID.msg());
                        return;
                    }
                }
            }
        }
        catch ( Exception ex ) {
            m_eWrapper.connectionClosed();
        }
    }

    String readStr() throws IOException {
        StringBuffer buf = new StringBuffer();
        while( true) {
            byte c = m_dis.readByte();
            if( c == 0) {
                break;
            }
            buf.append( (char)c);
        }

        String str = buf.toString();
        return str.length() == 0 ? null : str;
    }


    boolean readBoolFromInt() throws IOException {
        String str = readStr();
        return str == null ? false : (Integer.parseInt( str) != 0);
    }

    int readInt() throws IOException {
        String str = readStr();
        return str == null ? 0 : Integer.parseInt( str);
    }

    double readDouble() throws IOException {
        String str = readStr();
        return str == null ? 0 : Double.parseDouble( str);
    }
}
