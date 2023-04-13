
package com.ib.client;

import java.io.*;
import java.net.*;

public class EClientSocket {

    // Client version history
    //
    // 	6 = Added parentId to orderStatus
    // 	7 = The new execDetails event returned for an order filled status and reqExecDetails
    //     Also market depth is available.
    // 	8 = Added lastFillPrice to orderStatus() event and permId to execution details
    //  9 = Added 'averageCost', 'unrealizedPNL', and 'unrealizedPNL' to updatePortfolio event
    // 10 = Added 'serverId' to the 'open order' & 'order status' events.
    //      We send back all the API open orders upon connection.
    //      Added new methods reqAllOpenOrders, reqAutoOpenOrders()
    //      Added FA support - reqExecution has filter.
    //                       - reqAccountUpdates takes acct code.
    // 11 = Added permId to openOrder event.
    // 12 = requsting open order attributes ignoreRth, hidden, and discretionary
    // 13 = added goodAfterTime
    // 14 = always send size on bid/ask/last tick
    // 15 = send allocation description string on openOrder
    // 16 = can receive account name in account and portfolio updates, and fa params in openOrder
    // 17 = can receive liquidation field in exec reports, and notAutoAvailable field in mkt data
    // 18 = can receive good till date field in open order messages, and request intraday backfill
    // 19 = can receive rthOnly flag in ORDER_STATUS
    // 20 = expects TWS time string on connection after server version >= 20.
    // 21 = can receive bond contract details.
    // 22 = can receive price magnifier in version 2 contract details message
    // 23 = support for scanner
    // 24 = can receive volatility order parameters in open order messages
	// 25 = can receive HMDS query start and end times
	// 26 = can receive option vols in option market data messages
	// 27 = can receive delta neutral order type and delta neutral aux price

    private static final int CLIENT_VERSION = 27;
    private static final int SERVER_VERSION = 1;
    private static final byte[] EOL = {0};
    private static final String BAG_SEC_TYPE = "BAG";

    // FA msg data types
    public static final int GROUPS = 1;
    public static final int PROFILES = 2;
    public static final int ALIASES = 3;

    public static String faMsgTypeName(int faDataType) {
        switch (faDataType) {
            case GROUPS:
                return "GROUPS";
            case PROFILES:
                return "PROFILES";
            case ALIASES:
                return "ALIASES";
        }
        return null;
    }

    // outgoing msg id's
    private static final int REQ_MKT_DATA = 1;
    private static final int CANCEL_MKT_DATA = 2;
    private static final int PLACE_ORDER = 3;
    private static final int CANCEL_ORDER = 4;
    private static final int REQ_OPEN_ORDERS = 5;
    private static final int REQ_ACCOUNT_DATA = 6;
    private static final int REQ_EXECUTIONS = 7;
    private static final int REQ_IDS = 8;
    private static final int REQ_CONTRACT_DATA = 9;
    private static final int REQ_MKT_DEPTH = 10;
    private static final int CANCEL_MKT_DEPTH = 11;
    private static final int REQ_NEWS_BULLETINS = 12;
    private static final int CANCEL_NEWS_BULLETINS = 13;
    private static final int SET_SERVER_LOGLEVEL = 14;
    private static final int REQ_AUTO_OPEN_ORDERS = 15;
    private static final int REQ_ALL_OPEN_ORDERS = 16;
    private static final int REQ_MANAGED_ACCTS = 17;
    private static final int REQ_FA = 18;
    private static final int REPLACE_FA = 19;
    private static final int REQ_HISTORICAL_DATA = 20;
    private static final int EXERCISE_OPTIONS = 21;
    private static final int REQ_SCANNER_SUBSCRIPTION = 22;
    private static final int CANCEL_SCANNER_SUBSCRIPTION = 23;
    private static final int REQ_SCANNER_PARAMETERS = 24;
    private static final int CANCEL_HISTORICAL_DATA = 25;

    private EWrapper 			m_eWrapper;	// msg handler
    private Socket 			    m_socket;   // the socket
    private DataOutputStream 	m_dos;      // the socket output stream
    private boolean 			m_connected;// true if we are connected
    private EReader 			m_reader;   // thread which reads msgs from socket
    private int 			    m_serverVersion =1;
    private String              m_TwsTime;

    public int serverVersion()          { return m_serverVersion;   }
    public String TwsConnectionTime()   { return m_TwsTime; }
    protected EWrapper eWrapper() 		{ return m_eWrapper; }

    public EClientSocket( EWrapper eWrapper) {
        m_eWrapper = eWrapper;
    }

    public boolean isConnected() {
        return m_connected;
    }

    public synchronized void eConnect( String host, int port, int clientId) {
        // already connected?
        if( m_connected) {
            m_eWrapper.error(EClientErrors.NO_VALID_ID, EClientErrors.ALREADY_CONNECTED.code(),
                    EClientErrors.ALREADY_CONNECTED.msg());
            return;
        }

        try {
            // connect
            if( isNull( host) ) {
                host = "127.0.0.1";
            }
            m_socket = new Socket( host, port);

            // create io streams
            DataInputStream dis = new DataInputStream( m_socket.getInputStream() );
            m_dos = new DataOutputStream( m_socket.getOutputStream() );

            // set client version
            send( CLIENT_VERSION);

            // start reader thread
            m_reader = new EReader( this, dis);

            // check server version
            m_serverVersion = m_reader.readInt();
            System.out.println("Server Version:" + m_serverVersion);
            if ( m_serverVersion >= 20 ){
                m_TwsTime = m_reader.readStr();
                System.out.println("TWS Time at connection:" + m_TwsTime);
            }
            if( m_serverVersion < SERVER_VERSION) {
                m_eWrapper.error( EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(), EClientErrors.UPDATE_TWS.msg());
                return;
            }

            // Send the client id
            if ( m_serverVersion >= 3 ){
                send( clientId);
            }

            m_reader.start();

            // set connected flag
            m_connected = true;
        }
        catch( Exception e) {
            m_eWrapper.error( EClientErrors.NO_VALID_ID, EClientErrors.CONNECT_FAIL.code(),
                    EClientErrors.CONNECT_FAIL.msg());
        }
    }

    public synchronized void eDisconnect() {
        // not connected?
        if( !m_connected) {
            return;
        }

        try {
            // stop reader thread
            if( m_reader != null) {
                m_reader.interrupt();
            }

            // close socket
            if( m_socket != null) {
                m_socket.close();
            }
        }
        catch( Exception e) {
        }

        m_connected = false;
    }

    public synchronized void cancelScannerSubscription( int tickerId) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        if (m_serverVersion < 24) {
          error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                "  It does not support API scanner subscription.");
          return;
        }

        final int VERSION = 1;

        // send cancel mkt data msg
        try {
            send( CANCEL_SCANNER_SUBSCRIPTION);
            send( VERSION);
            send( tickerId);
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_CANSCANNER, "" + e);
            close();
        }
    }

    public synchronized void reqScannerParameters() {
        // not connected?
        if (!m_connected) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        if (m_serverVersion < 24) {
          error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                "  It does not support API scanner subscription.");
          return;
        }

        final int VERSION = 1;

        try {
            send(REQ_SCANNER_PARAMETERS);
            send(VERSION);
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID,
                   EClientErrors.FAIL_SEND_REQSCANNERPARAMETERS, "" + e);
            close();
        }
    }

    public synchronized void reqScannerSubscription( int tickerId,
        ScannerSubscription subscription) {
        // not connected?
        if (!m_connected) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        if (m_serverVersion < 24) {
          error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                "  It does not support API scanner subscription.");
          return;
        }

        final int VERSION = 3;

        try {
            send(REQ_SCANNER_SUBSCRIPTION);
            send(VERSION);
            send(tickerId);
            sendMax(subscription.numberOfRows());
            send(subscription.instrument());
            send(subscription.locationCode());
            send(subscription.scanCode());
            sendMax(subscription.abovePrice());
            sendMax(subscription.belowPrice());
            sendMax(subscription.aboveVolume());
            sendMax(subscription.marketCapAbove());
            sendMax(subscription.marketCapBelow());
            send(subscription.moodyRatingAbove());
            send(subscription.moodyRatingBelow());
            send(subscription.spRatingAbove());
            send(subscription.spRatingBelow());
            send(subscription.maturityDateAbove());
            send(subscription.maturityDateBelow());
            sendMax(subscription.couponRateAbove());
            sendMax(subscription.couponRateBelow());
            send(subscription.excludeConvertible());
            if (m_serverVersion >= 25) {
                send(subscription.averageOptionVolumeAbove());
                send(subscription.scannerSettingPairs());
            }
            if (m_serverVersion >= 27) {
                send(subscription.stockTypeFilter());
            }
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_REQSCANNER, "" + e);
            close();
        }
    }

    public synchronized void reqMktData(int tickerId, Contract contract) {
        // not connected?
        if (!m_connected) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 5;

        try {
            // send req mkt data msg
            send(REQ_MKT_DATA);
            send(VERSION);
            send(tickerId);

            send(contract.m_symbol);
            send(contract.m_secType);
            send(contract.m_expiry);
            send(contract.m_strike);
            send(contract.m_right);
            if (m_serverVersion >= 15) {
                send(contract.m_multiplier);
            }
            send(contract.m_exchange);
            if (m_serverVersion >= 14) {
                send(contract.m_primaryExch);
            }
            send(contract.m_currency);
            if(m_serverVersion >= 2) {
                send( contract.m_localSymbol);
            }
            if(m_serverVersion >= 8 && BAG_SEC_TYPE.equalsIgnoreCase(contract.m_secType)) {
                if ( contract.m_comboLegs == null ) {
                    send( 0);
                }
                else {
                    send( contract.m_comboLegs.size());

                    ComboLeg comboLeg;
                    for (int i=0; i < contract.m_comboLegs.size(); i ++) {
                        comboLeg = (ComboLeg)contract.m_comboLegs.get(i);
                        send( comboLeg.m_conId);
                        send( comboLeg.m_ratio);
                        send( comboLeg.m_action);
                        send( comboLeg.m_exchange);
                    }
                }
            }
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_REQMKT, "" + e);
            close();
        }
    }

    public synchronized void cancelHistoricalData( int tickerId ) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        if (m_serverVersion < 24) {
          error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                "  It does not support historical data query cancellation.");
          return;
        }

        final int VERSION = 1;

        // send cancel mkt data msg
        try {
            send( CANCEL_HISTORICAL_DATA);
            send( VERSION);
            send( tickerId);
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_CANSCANNER, "" + e);
            close();
        }
    }

    public synchronized void reqHistoricalData( int tickerId, Contract contract,
                                                String endDateTime, String durationStr,
                                                int barSizeSetting, String whatToShow,
                                                int useRTH, int formatDate) {
        // not connected?
        if( !m_connected) {
            error( tickerId, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 3;

        try {
          if (m_serverVersion < 16) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                  "  It does not support historical data backfill.");
            return;
          }

          send(REQ_HISTORICAL_DATA);
          send(VERSION);
          send(tickerId);
          send(contract.m_symbol);
          send(contract.m_secType);
          send(contract.m_expiry);
          send(contract.m_strike);
          send(contract.m_right);
          send(contract.m_multiplier);
          send(contract.m_exchange);
          send(contract.m_primaryExch);
          send(contract.m_currency);
          send(contract.m_localSymbol);
          if (m_serverVersion >= 20) {
              send(endDateTime);
              send(barSizeSetting);
          }
          send(durationStr);
          send(useRTH);
          send(whatToShow);
          if (m_serverVersion > 16) {
              send(formatDate);
          }
          if (BAG_SEC_TYPE.equalsIgnoreCase(contract.m_secType)) {
              if (contract.m_comboLegs == null) {
                  send(0);
              }
              else {
                  send(contract.m_comboLegs.size());

                  ComboLeg comboLeg;
                  for (int i = 0; i < contract.m_comboLegs.size(); i++) {
                      comboLeg = (ComboLeg) contract.m_comboLegs.get(i);
                      send(comboLeg.m_conId);
                      send(comboLeg.m_ratio);
                      send(comboLeg.m_action);
                      send(comboLeg.m_exchange);
                  }
              }
          }
        }
        catch (Exception e) {
          error(tickerId, EClientErrors.FAIL_SEND_REQMKT, "" + e);
          close();
        }
    }

    public synchronized void reqContractDetails(Contract contract)
    {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        // This feature is only available for versions of TWS >=4
        if( m_serverVersion < 4) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(),
                            EClientErrors.UPDATE_TWS.msg());
            return;
        }

        final int VERSION = 2;

        try {
            // send req mkt data msg
            send( REQ_CONTRACT_DATA);
            send( VERSION);

            send( contract.m_symbol);
            send( contract.m_secType);
            send( contract.m_expiry);
            send( contract.m_strike);
            send( contract.m_right);
            if (m_serverVersion >= 15) {
                send(contract.m_multiplier);
            }
            send( contract.m_exchange);
            send( contract.m_currency);
            send( contract.m_localSymbol);
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_REQCONTRACT, "" + e);
            close();
        }
    }

    public synchronized void reqMktDepth( int tickerId, Contract contract, int numRows)
    {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        // This feature is only available for versions of TWS >=6
        if( m_serverVersion < 6) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(),
                    EClientErrors.UPDATE_TWS.msg());
            return;
        }

        final int VERSION = 3;

        try {
            // send req mkt data msg
            send( REQ_MKT_DEPTH);
            send( VERSION);
            send( tickerId);

            send( contract.m_symbol);
            send( contract.m_secType);
            send( contract.m_expiry);
            send( contract.m_strike);
            send( contract.m_right);
            if (m_serverVersion >= 15) {
              send(contract.m_multiplier);
            }
            send( contract.m_exchange);
            send( contract.m_currency);
            send( contract.m_localSymbol);
            if (m_serverVersion >= 19) {
                send( numRows);
            }
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_REQMKTDEPTH, "" + e);
            close();
        }
    }

    public synchronized void cancelMktData( int tickerId) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send cancel mkt data msg
        try {
            send( CANCEL_MKT_DATA);
            send( VERSION);
            send( tickerId);
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_CANMKT, "" + e);
            close();
        }
    }

    public synchronized void cancelMktDepth( int tickerId) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        // This feature is only available for versions of TWS >=6
        if( m_serverVersion < 6) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(),
                    EClientErrors.UPDATE_TWS.msg());
            return;
        }

        final int VERSION = 1;

        // send cancel mkt data msg
        try {
            send( CANCEL_MKT_DEPTH);
            send( VERSION);
            send( tickerId);
        }
        catch( Exception e) {
            error( tickerId, EClientErrors.FAIL_SEND_CANMKTDEPTH, "" + e);
            close();
        }
    }

    public synchronized void exerciseOptions( int tickerId, Contract contract,
                                              int exerciseAction, int exerciseQuantity,
                                              String account, int override) {
        // not connected?
        if( !m_connected) {
            error( tickerId, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        try {
          if (m_serverVersion < 21) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS,
                  "  It does not support options exercise from the API.");
            return;
          }

          send(EXERCISE_OPTIONS);
          send(VERSION);
          send(tickerId);
          send(contract.m_symbol);
          send(contract.m_secType);
          send(contract.m_expiry);
          send(contract.m_strike);
          send(contract.m_right);
          send(contract.m_multiplier);
          send(contract.m_exchange);
          send(contract.m_currency);
          send(contract.m_localSymbol);
          send(exerciseAction);
          send(exerciseQuantity);
          send(account);
          send(override);
      }
      catch (Exception e) {
        error(tickerId, EClientErrors.FAIL_SEND_REQMKT, "" + e);
        close();
      }
    }

    public synchronized void placeOrder( int id, Contract contract, Order order) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 20;

        // send place order msg
        try {
            send( PLACE_ORDER);
            send( VERSION);
            send( id);

            // send contract fields
            send( contract.m_symbol);
            send( contract.m_secType);
            send( contract.m_expiry);
            send( contract.m_strike);
            send( contract.m_right);
            if (m_serverVersion >= 15) {
                send(contract.m_multiplier);
            }
            send( contract.m_exchange);
            if( m_serverVersion >= 14) {
              send(contract.m_primaryExch);
            }
            send( contract.m_currency);
            if( m_serverVersion >= 2) {
                send (contract.m_localSymbol);
            }

            // send main order fields
            send( order.m_action);
            send( order.m_totalQuantity);
            send( order.m_orderType);
            send( order.m_lmtPrice);
            send( order.m_auxPrice);

            // send extended order fields
            send( order.m_tif);
            send( order.m_ocaGroup);
            send( order.m_account);
            send( order.m_openClose);
            send( order.m_origin);
            send( order.m_orderRef);
            send( order.m_transmit);
            if( m_serverVersion >= 4 ) {
                send (order.m_parentId);
            }

            if( m_serverVersion >= 5 ) {
                send (order.m_blockOrder);
                send (order.m_sweepToFill);
                send (order.m_displaySize);
                send (order.m_triggerMethod);
                send (order.m_ignoreRth);
            }

            if(m_serverVersion >= 7 ) {
                send(order.m_hidden);
            }

            // Send combo legs for BAG requests
            if(m_serverVersion >= 8 && BAG_SEC_TYPE.equalsIgnoreCase(contract.m_secType)) {
                if ( contract.m_comboLegs == null ) {
                    send( 0);
                }
                else {
                    send( contract.m_comboLegs.size());

                    ComboLeg comboLeg;
                    for (int i=0; i < contract.m_comboLegs.size(); i ++) {
                        comboLeg = (ComboLeg)contract.m_comboLegs.get(i);
                        send( comboLeg.m_conId);
                        send( comboLeg.m_ratio);
                        send( comboLeg.m_action);
                        send( comboLeg.m_exchange);
                        send( comboLeg.m_openClose);
                    }
                }
            }

            if ( m_serverVersion >= 9 ) {
                send( order.m_sharesAllocation); // deprecated
            }

            if ( m_serverVersion >= 10 ) {
                send( order.m_discretionaryAmt);
            }

            if ( m_serverVersion >= 11 ) {
                send( order.m_goodAfterTime);
            }

            if ( m_serverVersion >= 12 ) {
                send( order.m_goodTillDate);
            }

            if ( m_serverVersion >= 13 ) {
               send( order.m_faGroup);
               send( order.m_faMethod);
               send( order.m_faPercentage);
               send( order.m_faProfile);
           }
           if (m_serverVersion >= 18) { // institutional short sale slot fields.
               send( order.m_shortSaleSlot);      // 0 only for retail, 1 or 2 only for institution.
               send( order.m_designatedLocation); // only populate when order.m_shortSaleSlot = 2.
           }
           if (m_serverVersion >= 19) {
               send( order.m_ocaType);
               send( order.m_rthOnly);
               send( order.m_rule80A);
               send( order.m_settlingFirm);
               send( order.m_allOrNone);
               sendMax( order.m_minQty);
               sendMax( order.m_percentOffset);
               send( order.m_eTradeOnly);
               send( order.m_firmQuoteOnly);
               sendMax( order.m_nbboPriceCap);
               sendMax( order.m_auctionStrategy);
               sendMax( order.m_startingPrice);
               sendMax( order.m_stockRefPrice);
               sendMax( order.m_delta);
        	   // Volatility orders had specific watermark price attribs in server version 26
        	   double lower = (m_serverVersion == 26 && order.m_orderType.equals("VOL"))
        	   		? Double.MAX_VALUE
        	   		: order.m_stockRangeLower;
        	   double upper = (m_serverVersion == 26 && order.m_orderType.equals("VOL"))
   	   				? Double.MAX_VALUE
   	   				: order.m_stockRangeUpper;
               sendMax( lower);
               sendMax( upper);
           }
           if (m_serverVersion >= 22) {
               send( order.m_overridePercentageConstraints);
           }
           
           if (m_serverVersion >= 26) { // Volatility orders
               sendMax( order.m_volatility);
               sendMax( order.m_volatilityType);
               if (m_serverVersion < 28) {
            	   send( order.m_deltaNeutralOrderType.equalsIgnoreCase("MKT"));
               } else {
            	   send( order.m_deltaNeutralOrderType);
            	   sendMax( order.m_deltaNeutralAuxPrice);
               }
               send( order.m_continuousUpdate);
               if (m_serverVersion == 26) {
            	   // Volatility orders had specific watermark price attribs in server version 26
            	   double lower = order.m_orderType.equals("VOL") ? order.m_stockRangeLower : Double.MAX_VALUE;
            	   double upper = order.m_orderType.equals("VOL") ? order.m_stockRangeUpper : Double.MAX_VALUE;
                   sendMax( lower);
                   sendMax( upper);
               }
               sendMax( order.m_referencePriceType);
           }
        }
        catch( Exception e) {
            error( id, EClientErrors.FAIL_SEND_ORDER, "" + e);
            close();
        }
    }

    public synchronized void reqAccountUpdates(boolean subscribe, String acctCode) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 2;

        // send cancel order msg
        try {
            send( REQ_ACCOUNT_DATA );
            send( VERSION);
            send( subscribe);

            // Send the account code. This will only be used for FA clients
            if ( m_serverVersion >= 9 ) {
                send( acctCode);
            }
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_ACCT, "" + e);
            close();
        }
    }

    public synchronized void reqExecutions(ExecutionFilter filter) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 2;

        // send cancel order msg
        try {
            send( REQ_EXECUTIONS);
            send( VERSION);

            // Send the execution rpt filter data
            if ( m_serverVersion >= 9 ) {
                send( filter.m_clientId);
                send( filter.m_acctCode);

                // Note that the valid format for m_time is "yyyymmdd-hh:mm:ss"
                send( filter.m_time);
                send( filter.m_symbol);
                send( filter.m_secType);
                send( filter.m_exchange);
                send( filter.m_side);
            }
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_EXEC, "" + e);
            close();
        }
    }

    public synchronized void cancelOrder( int id) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send cancel order msg
        try {
            send( CANCEL_ORDER);
            send( VERSION);
            send( id);
        }
        catch( Exception e) {
            error( id, EClientErrors.FAIL_SEND_CORDER, "" + e);
            close();
        }
    }

    public synchronized void reqOpenOrders() {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send cancel order msg
        try {
            send( REQ_OPEN_ORDERS);
            send( VERSION);
        }
        catch( Exception e) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_OORDER, "" + e);
            close();
        }
    }

    public synchronized void reqIds( int numIds) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        try {
            send( REQ_IDS);
            send( VERSION);
            send( numIds);
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_CORDER, "" + e);
            close();
        }
    }

    public synchronized void reqNewsBulletins( boolean allMsgs) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        try {
            send( REQ_NEWS_BULLETINS);
            send( VERSION);
            send( allMsgs);
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_CORDER, "" + e);
            close();
        }
    }

    public synchronized void cancelNewsBulletins() {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send cancel order msg
        try {
            send( CANCEL_NEWS_BULLETINS);
            send( VERSION);
        }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_CORDER, "" + e);
            close();
        }
    }

    public synchronized void setServerLogLevel(int logLevel) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

                // send the set server logging level message
                try {
                        send( SET_SERVER_LOGLEVEL);
                        send( VERSION);
                        send( logLevel);
                }
        catch( Exception e) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_SERVER_LOG_LEVEL, "" + e);
            close();
        }
    }

    public synchronized void reqAutoOpenOrders(boolean bAutoBind)
    {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send req open orders msg
        try {
            send( REQ_AUTO_OPEN_ORDERS);
            send( VERSION);
            send( bAutoBind);
        }
        catch( Exception e) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_OORDER, "" + e);
            close();
        }
    }

    public synchronized void reqAllOpenOrders() {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send req all open orders msg
        try {
            send( REQ_ALL_OPEN_ORDERS);
            send( VERSION);
        }
        catch( Exception e) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_OORDER, "" + e);
            close();
        }
    }

    public synchronized void reqManagedAccts() {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        final int VERSION = 1;

        // send req FA managed accounts msg
        try {
            send( REQ_MANAGED_ACCTS);
            send( VERSION);
        }
        catch( Exception e) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.FAIL_SEND_OORDER, "" + e);
            close();
        }
    }

    public synchronized void requestFA( int faDataType ) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        // This feature is only available for versions of TWS >= 13
        if( m_serverVersion < 13) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(),
                    EClientErrors.UPDATE_TWS.msg());
            return;
        }

        final int VERSION = 1;

        try {
            send( REQ_FA );
            send( VERSION);
            send( faDataType);
        }
        catch( Exception e) {
            error( faDataType, EClientErrors.FAIL_SEND_FA_REQUEST, "" + e);
            close();
        }
    }

    public synchronized void replaceFA( int faDataType, String xml ) {
        // not connected?
        if( !m_connected) {
            error( EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED, "");
            return;
        }

        // This feature is only available for versions of TWS >= 13
        if( m_serverVersion < 13) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(),
                    EClientErrors.UPDATE_TWS.msg());
            return;
        }

        final int VERSION = 1;

        try {
            send( REPLACE_FA );
            send( VERSION);
            send( faDataType);
            send( xml);
        }
        catch( Exception e) {
            error( faDataType, EClientErrors.FAIL_SEND_FA_REPLACE, "" + e);
            close();
        }
    }

    protected synchronized void error( String err) {
        m_eWrapper.error( err);
    }

    protected synchronized void error( int id, int errorCode, String errorMsg) {
        m_eWrapper.error( id, errorCode, errorMsg);
    }

    protected void close() {
        eDisconnect();
        m_eWrapper.connectionClosed();
    }

    private static boolean is( String str) {
        // return true if the string is not empty
        return str != null && str.length() > 0;
    }

    private static boolean isNull( String str) {
        // return true if the string is null or empty
        return !is( str);
    }

    private void error(int id, EClientErrors.CodeMsgPair pair, String tail) {
        error(id, pair.code(), pair.msg() + tail);
    }

    private void send( String str) throws IOException {
        // write string to data buffer; writer thread will
        // write it to socket
        if( str != null) {
            m_dos.write( str.getBytes() );
        }
        sendEOL();
    }

    private void sendEOL() throws IOException {
        m_dos.write( EOL);
    }

    private void send( int val) throws IOException {
        send( String.valueOf( val) );
    }

    private void send( char val) throws IOException {
        m_dos.write( val);
        sendEOL();
    }

    private void send( double val) throws IOException {
        send( String.valueOf( val) );
    }

    private void send( long val) throws IOException {
        send( String.valueOf( val) );
    }

    private void sendMax( double val) throws IOException {
        if (val == Double.MAX_VALUE) {
            sendEOL();
        }
        else {
            send(String.valueOf(val));
        }
    }

    private void sendMax( int val) throws IOException {
        if (val == Integer.MAX_VALUE) {
            sendEOL();
        }
        else {
            send(String.valueOf(val));
        }
    }

    private void send( boolean val) throws IOException {
        send( val ? 1 : 0);
    }
}
