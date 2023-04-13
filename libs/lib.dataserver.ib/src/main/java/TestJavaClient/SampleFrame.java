
package TestJavaClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.TickType;

public class SampleFrame extends JFrame implements EWrapper {
    private static final int NOT_AN_FA_ACCOUNT_ERROR = 321 ;
    private int faErrorCodes[] = { 503, 504, 505, 522, 1100, NOT_AN_FA_ACCOUNT_ERROR } ;
    private boolean faError ;

    EClientSocket   m_client = new EClientSocket( this);
    IBTextPanel     m_tickers = new IBTextPanel("Market and Historical Data", false);
    IBTextPanel     m_TWS = new IBTextPanel("TWS Server Responses", false);
    IBTextPanel     m_errors = new IBTextPanel("Errors and Messages", false);
    OrderDlg        m_orderDlg = new OrderDlg( this);
    ExtOrdDlg       m_extOrdDlg = new ExtOrdDlg( m_orderDlg);
    AccountDlg      m_acctDlg = new AccountDlg(this);
    MktDepthDlg     m_mktDepthDlg = new MktDepthDlg(this);
    NewsBulletinDlg m_newsBulletinDlg = new NewsBulletinDlg(this);
    ScannerDlg      m_scannerDlg = new ScannerDlg(this);

    String faGroupXML ;
    String faProfilesXML ;
    String faAliasesXML ;
    public String   m_FAAcctCodes;
    public boolean  m_bIsFAAccount = false;

    public SampleFrame() {
        JPanel scrollingWindowDisplayPanel = new JPanel( new GridLayout( 0, 1) );
        scrollingWindowDisplayPanel.add( m_tickers);
        scrollingWindowDisplayPanel.add( m_TWS);
        scrollingWindowDisplayPanel.add( m_errors);

        JPanel buttonPanel = createButtonPanel();

        getContentPane().add( scrollingWindowDisplayPanel, BorderLayout.CENTER);
        getContentPane().add( buttonPanel, BorderLayout.EAST);
        setSize( 600, 700);
        setTitle( "Sample");
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel( new GridLayout( 0, 1) );
        JButton butConnect = new JButton( "Connect");
        butConnect.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onConnect();
            }
        });
        JButton butDisconnect = new JButton( "Disconnect");
        butDisconnect.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onDisconnect();
            }
        });
        JButton butMktData = new JButton( "Req Mkt Data");
        butMktData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqMktData();
            }
        });
        JButton butCancelMktData = new JButton( "Cancel Mkt Data");
        butCancelMktData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onCancelMktData();
            }
        });
        JButton butMktDepth = new JButton( "Req Mkt Depth");
        butMktDepth.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqMktDepth();
            }
        });
        JButton butCancelMktDepth = new JButton( "Cancel Mkt Depth");
        butCancelMktDepth.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onCancelMktDepth();
            }
        });
        JButton butHistoricalData = new JButton( "Historical Data");
        butHistoricalData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onHistoricalData();
            }
        });
        JButton butCancelHistoricalData = new JButton( "Cancel Hist. Data");
        butCancelHistoricalData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onCancelHistoricalData();
            }
        });
        JButton butScanner = new JButton( "Market Scanner");
        butScanner.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onScanner();
            }
        });
        JButton butOpenOrders = new JButton( "Req Open Orders");
        butOpenOrders.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqOpenOrders();
            }
        });
        JButton butPlaceOrder = new JButton( "Place Order");
        butPlaceOrder.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onPlaceOrder();
            }
        });
        JButton butCancelOrder = new JButton( "Cancel Order");
        butCancelOrder.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onCancelOrder();
            }
        });
        JButton butExerciseOptions = new JButton( "Exercise Options");
        butExerciseOptions.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onExerciseOptions();
            }
        });
        JButton butExtendedOrder = new JButton( "Extended");
        butExtendedOrder.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onExtendedOrder();
            }
        });
        JButton butAcctData = new JButton( "Req Acct Data");
        butAcctData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqAcctData();
            }
        });
        JButton butContractData = new JButton( "Req Contract Data");
        butContractData.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqContractData();
            }
        });
        JButton butExecutions = new JButton( "Req Executions");
        butExecutions.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqExecutions();
            }
        });
        JButton butNewsBulletins = new JButton( "Req News Bulletins");
        butNewsBulletins.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqNewsBulletins();
            }
        });
        JButton butServerLogging = new JButton( "Server Logging");
        butServerLogging.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onServerLogging();
            }
        });
        JButton butAllOpenOrders = new JButton( "Req All Open Orders");
        butAllOpenOrders.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqAllOpenOrders();
            }
        });
        JButton butAutoOpenOrders = new JButton( "Req Auto Open Orders");
        butAutoOpenOrders.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqAutoOpenOrders();
            }
        });
        JButton butManagedAccts = new JButton( "Req Accounts");
        butManagedAccts.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onReqManagedAccts();
            }
        });
        JButton butFinancialAdvisor = new JButton( "Financial Advisor");
        butFinancialAdvisor.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onFinancialAdvisor();
            }
        });
        JButton butClear = new JButton( "Clear");
        butClear.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onClear();
            }
        });
        JButton butClose = new JButton( "Close");
        butClose.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onClose();
            }
        });


        buttonPanel.add( new JPanel() );
        buttonPanel.add( butConnect);
        buttonPanel.add( butDisconnect);

        buttonPanel.add( new JPanel() );
        buttonPanel.add( butMktData);
        buttonPanel.add( butCancelMktData);
        buttonPanel.add( butMktDepth);
        buttonPanel.add( butCancelMktDepth);
        buttonPanel.add( butHistoricalData);
        buttonPanel.add( butCancelHistoricalData);
        buttonPanel.add( butScanner);

        buttonPanel.add( new JPanel() );
        buttonPanel.add( butPlaceOrder);
        buttonPanel.add( butCancelOrder);
        buttonPanel.add( butExerciseOptions);
        buttonPanel.add( butExtendedOrder);

        buttonPanel.add( new JPanel() );
        buttonPanel.add( butContractData );
        buttonPanel.add( butOpenOrders);
        buttonPanel.add( butAllOpenOrders);
        buttonPanel.add( butAutoOpenOrders);
        buttonPanel.add( butAcctData );
        buttonPanel.add( butExecutions );
        buttonPanel.add( butNewsBulletins );
        buttonPanel.add( butServerLogging );
        buttonPanel.add( butManagedAccts );
        buttonPanel.add( butFinancialAdvisor ) ;

        buttonPanel.add( new JPanel() );
        buttonPanel.add( butClear );
        buttonPanel.add( butClose );
        
        return buttonPanel;
    }
    
    void onConnect() {
        m_bIsFAAccount = false;
        // get connection parameters
        ConnectDlg dlg = new ConnectDlg( this);
        dlg.show();
        if( !dlg.m_rc) {
            return;
        }

        // connect to TWS
        m_client.eConnect( dlg.m_retIpAddress, dlg.m_retPort, dlg.m_retClientId);
        if (m_client.isConnected()) {
            m_TWS.add("Connected to Tws server version " +
                       m_client.serverVersion() + " at " +
                       m_client.TwsConnectionTime());
        }
    }

    void onDisconnect() {
        // disconnect from TWS
        m_client.eDisconnect();
    }

    void onReqMktData() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // req mkt data
        m_client.reqMktData( m_orderDlg.m_id, m_orderDlg.m_contract );
    }

    void onScanner() {
        m_scannerDlg.show();
        if (m_scannerDlg.m_userSelection == ScannerDlg.CANCEL_SELECTION) {
            m_client.cancelScannerSubscription(m_scannerDlg.m_id);
        }
        else if (m_scannerDlg.m_userSelection == ScannerDlg.SUBSCRIBE_SELECTION) {
            m_client.reqScannerSubscription(m_scannerDlg.m_id,
                                            m_scannerDlg.m_subscription);
        }
        else if (m_scannerDlg.m_userSelection == ScannerDlg.REQUEST_PARAMETERS_SELECTION) {
            m_client.reqScannerParameters();
        }
    }

    void onHistoricalData() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // req historical data
        m_client.reqHistoricalData( m_orderDlg.m_id, m_orderDlg.m_contract,
                                    m_orderDlg.m_backfillEndTime, m_orderDlg.m_backfillDuration,
                                    m_orderDlg.m_barSizeSetting, m_orderDlg.m_whatToShow,
                                    m_orderDlg.m_useRTH, m_orderDlg.m_formatDate );
    }

    void onCancelHistoricalData() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // cancel historical data
        m_client.cancelHistoricalData( m_orderDlg.m_id );
    }

    void onReqContractData() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // req mkt data
        m_client.reqContractDetails( m_orderDlg.m_contract );
    }

    void onReqMktDepth() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        m_mktDepthDlg.setParams( m_client, m_orderDlg.m_id);

        // req mkt data
        m_client.reqMktDepth( m_orderDlg.m_id, m_orderDlg.m_contract, m_orderDlg.m_marketDepthRows );
        m_mktDepthDlg.show();
    }

    void onCancelMktData() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // cancel market data
        m_client.cancelMktData( m_orderDlg.m_id );
    }

    void onCancelMktDepth() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // cancel market data
        m_client.cancelMktDepth( m_orderDlg.m_id );
    }

    void onReqOpenOrders() {
        m_client.reqOpenOrders();
    }

    void onPlaceOrder() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // place order
        m_client.placeOrder( m_orderDlg.m_id, m_orderDlg.m_contract, m_orderDlg.m_order );
    }

    void onExerciseOptions() {
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // cancel order
        m_client.exerciseOptions( m_orderDlg.m_id, m_orderDlg.m_contract,
                                  m_orderDlg.m_exerciseAction, m_orderDlg.m_exerciseQuantity,
                                  m_orderDlg.m_order.m_account, m_orderDlg.m_override);
    }

    void onCancelOrder() {
        // run m_orderDlg
        m_orderDlg.show();
        if( !m_orderDlg.m_rc ) {
            return;
        }

        // cancel order
        m_client.cancelOrder( m_orderDlg.m_id );
    }

    void onExtendedOrder() {
        //Show the extended order attributes dialog
        m_extOrdDlg.show();
        if( !m_extOrdDlg.m_rc ) {
            return;
        }

        // Copy over the extended order details
        copyExtendedOrderDetails( m_orderDlg.m_order, m_extOrdDlg.m_order);
    }

    void  onReqAcctData() {
        AcctUpdatesDlg dlg = new AcctUpdatesDlg(this);

        dlg.show();
        m_client.reqAccountUpdates( dlg.m_subscribe, dlg.m_acctCode);
        if ( m_client.isConnected() && dlg.m_subscribe) {
            m_acctDlg.reset();
            m_acctDlg.setVisible(true);
        }
    }

    void onFinancialAdvisor() {
      faGroupXML = faProfilesXML = faAliasesXML = null ;
      faError = false ;
      m_client.requestFA(EClientSocket.GROUPS) ;
      m_client.requestFA(EClientSocket.PROFILES) ;
      m_client.requestFA(EClientSocket.ALIASES) ;
    }

    void  onServerLogging() {
        // get server logging level
        LogConfigDlg dlg = new LogConfigDlg( this);
        dlg.show();
        if( !dlg.m_rc) {
            return;
        }

        // connect to TWS
        m_client.setServerLogLevel( dlg.m_serverLogLevel);
    }

    void  onReqAllOpenOrders() {
        // request list of all open orders
        m_client.reqAllOpenOrders();
    }

    void  onReqAutoOpenOrders() {
        // request to automatically bind any newly entered TWS orders
        // to this API client. NOTE: TWS orders can only be bound to
        // client's with clientId=0.
        m_client.reqAutoOpenOrders( true);
    }

    void  onReqManagedAccts() {
        // request the list of managed accounts
        m_client.reqManagedAccts();
    }

    void onClear() {
        m_tickers.clear();
        m_TWS.clear();
        m_errors.clear();
    }

    void onClose() {
        System.exit(1);
    }

    void onReqExecutions() {
        ExecFiliterDlg dlg = new ExecFiliterDlg(this);

        dlg.show();
        if ( dlg.m_rc ) {
            // request execution reports based on the supplied filter criteria
            m_client.reqExecutions( dlg.m_execFilter);
        }
    }

    void onReqNewsBulletins() {
        // run m_newsBulletinDlg
        m_newsBulletinDlg.show();
        if( !m_newsBulletinDlg.m_rc ) {
            return;
        }

        if ( m_newsBulletinDlg.m_subscribe ) {
            m_client.reqNewsBulletins( m_newsBulletinDlg.m_allMsgs);
        }
        else {
            m_client.cancelNewsBulletins();
        }
    }

    public void tickPrice( int tickerId, int field, double price, int canAutoExecute) {
        // received price tick
        m_tickers.add( "id=" + tickerId + "  " + TickType.getField( field) + "=" + price + " " + 
                        ((canAutoExecute != 0) ? " canAutoExecute" : " noAutoExecute") );
    }

    public void tickOptionComputation( int tickerId, int field, double impliedVol, double delta) {
        // received price tick
        m_tickers.add( "id=" + tickerId + "  " + TickType.getField( field) + ": vol = " + 
 			   ((impliedVol >= 0 && impliedVol != Double.MAX_VALUE) ? Double.toString(impliedVol) : "N/A") + " delta = " +
			   ((Math.abs(delta) <= 1) ? Double.toString(delta) : "N/A") );
    }

    public void tickSize( int tickerId, int field, int size) {
        // received size tick
        m_tickers.add( "id=" + tickerId + "  " + TickType.getField( field) + "=" + size);
    }

    public void orderStatus( int orderId, String status, int filled, int remaining,
        double avgFillPrice, int permId, int parentId, double lastFillPrice,
        int clientId) {
        // received order status
        m_TWS.add(  "order status: orderId=" + orderId + " clientId=" + clientId + " permId=" + permId +
                        " status=" + status + " filled=" + filled + " remaining=" + remaining +
                        " avgFillPrice=" + avgFillPrice + " lastFillPrice=" + lastFillPrice +
                        " parent Id=" + parentId);

        // make sure id for next order is at least orderId+1
        m_orderDlg.setIdAtLeast( orderId + 1);
    }

    public void openOrder( int orderId, Contract contract, Order order) {
        // received open order
        m_TWS.add( "open order: orderId=" + orderId +
                       " action=" + order.m_action +
                       " quantity=" + order.m_totalQuantity +
                       " symbol=" + contract.m_symbol +
                       " type=" + order.m_orderType +
                       " lmtPrice=" + order.m_lmtPrice +
                       " auxPrice=" + order.m_auxPrice +
                       " TIF=" + order.m_tif +
                       " localSymbol=" + contract.m_localSymbol +
                       " client Id=" + order.m_clientId +
                       " parent Id=" + order.m_parentId +
                       " permId=" + order.m_permId +
                       " ignoreRth=" + order.m_ignoreRth +
                       " hidden=" + order.m_hidden +
                       " discretionaryAmt=" + order.m_discretionaryAmt +
                       " triggerMethod=" + order.m_triggerMethod +
                       " goodAfterTime=" + order.m_goodAfterTime +
                       " goodTillDate=" + order.m_goodTillDate +
                       " account=" + order.m_account +
                       " allocation=" + order.m_sharesAllocation +
                       " faGroup=" + order.m_faGroup +
                       " faMethod=" + order.m_faMethod +
                       " faPercentage=" + order.m_faPercentage +
                       " faProfile=" + order.m_faProfile +
                       " shortSaleSlot=" + order.m_shortSaleSlot +
                       " designatedLocation=" + order.m_designatedLocation +
                       " ocaType=" + order.m_ocaType +
                       " rthOnly=" + order.m_rthOnly +
                       " rule80A=" + order.m_rule80A +
                       " settlingFirm=" + order.m_settlingFirm +
                       " allOrNone=" + order.m_allOrNone +
                       " minQty=" + order.m_minQty +
                       " percentOffset=" + order.m_percentOffset +
                       " eTradeOnly=" + order.m_eTradeOnly +
                       " firmQuoteOnly=" + order.m_firmQuoteOnly +
                       " nbboPriceCap=" + order.m_nbboPriceCap +
                       " auctionStrategy=" + order.m_auctionStrategy +
                       " startingPrice=" + order.m_startingPrice +
                       " stockRefPrice=" + order.m_stockRefPrice +
                       " delta=" + order.m_delta +
                       " stockRangeLower=" + order.m_stockRangeLower +
                       " stockRangeUpper=" + order.m_stockRangeUpper +
                       " volatility=" + order.m_volatility +
                       " volatilityType=" + order.m_volatilityType +
                       " deltaNeutralOrderType=" + order.m_deltaNeutralOrderType +
                       " deltaNeutralAuxPrice=" + order.m_deltaNeutralAuxPrice +
                       " continuousUpdate=" + order.m_continuousUpdate +
                       " referencePriceType=" + order.m_referencePriceType);
    }

    public void contractDetails(ContractDetails contractDetails)
    {
        Contract contract = contractDetails.m_summary;

        m_TWS.add( " ---- Contract Details begin ----");
        m_TWS.add( "symbol = " + contract.m_symbol);
        m_TWS.add( "secType = " + contract.m_secType);
        m_TWS.add( "expiry = " + contract.m_expiry);
        m_TWS.add( "strike = " + contract.m_strike);
        m_TWS.add( "right = " + contract.m_right);
        m_TWS.add( "exchange = " + contract.m_exchange);
        m_TWS.add( "currency = " + contract.m_currency);
        m_TWS.add( "localSymbol = " + contract.m_localSymbol);
        m_TWS.add( "marketName = " + contractDetails.m_marketName);
        m_TWS.add( "tradingClass = " + contractDetails.m_tradingClass);
        m_TWS.add( "conid = " + contractDetails.m_conid);
        m_TWS.add( "minTick = " + contractDetails.m_minTick);
        m_TWS.add( "multiplier = " + contractDetails.m_multiplier);
        m_TWS.add( "price magnifier = " + contractDetails.m_priceMagnifier);
        m_TWS.add( "orderTypes = " + contractDetails.m_orderTypes);
        m_TWS.add( "validExchanges = " + contractDetails.m_validExchanges);
        m_TWS.add(" ---- Contract Details End ----");
    }

    public void scannerData(int reqId, int rank,
                            ContractDetails contractDetails, String distance,
                            String benchmark, String projection) {
        Contract contract = contractDetails.m_summary;

        m_tickers.add("id = " + reqId +
                   " rank=" + rank +
                   " symbol=" + contract.m_symbol +
                   " secType=" + contract.m_secType +
                   " expiry=" + contract.m_expiry +
                   " strike=" + contract.m_strike +
                   " right=" + contract.m_right +
                   " exchange=" + contract.m_exchange +
                   " currency=" + contract.m_currency +
                   " localSymbol=" + contract.m_localSymbol +
                   " marketName=" + contractDetails.m_marketName +
                   " tradingClass=" + contractDetails.m_tradingClass +
                   " distance=" + distance +
                   " benchmark=" + benchmark +
                   " projection=" + projection);
    }

    public void bondContractDetails(ContractDetails contractDetails)
    {
        Contract contract = contractDetails.m_summary;

        m_TWS.add(" ---- Bond Contract Details begin ----");
        m_TWS.add("symbol = " + contract.m_symbol);
        m_TWS.add("secType = " + contract.m_secType);
        m_TWS.add("cusip = " + contract.m_cusip);
        m_TWS.add("coupon = " + contract.m_coupon);
        m_TWS.add("maturity = " + contract.m_maturity);
        m_TWS.add("issueDate = " + contract.m_issueDate);
        m_TWS.add("ratings = " + contract.m_ratings);
        m_TWS.add("bondType = " + contract.m_bondType);
        m_TWS.add("couponType = " + contract.m_couponType);
        m_TWS.add("convertible = " + contract.m_convertible);
        m_TWS.add("callable = " + contract.m_callable);
        m_TWS.add("putable = " + contract.m_putable);
        m_TWS.add("descAppend = " + contract.m_descAppend);
        m_TWS.add("exchange = " + contract.m_exchange);
        m_TWS.add("currency = " + contract.m_currency);
        m_TWS.add("marketName = " + contractDetails.m_marketName);
        m_TWS.add("tradingClass = " + contractDetails.m_tradingClass);
        m_TWS.add("conid = " + contractDetails.m_conid);
        m_TWS.add("minTick = " + contractDetails.m_minTick);
        m_TWS.add("orderTypes = " + contractDetails.m_orderTypes);
        m_TWS.add("validExchanges = " + contractDetails.m_validExchanges);
        m_TWS.add(" ---- Bond Contract Details End ----");
    }

    public void execDetails(int orderId, Contract contract, Execution execution)
    {
        m_TWS.add( " ---- Execution Details begin ----");
        m_TWS.add( "orderId = " + Integer.toString(orderId));
        m_TWS.add( "clientId = " + Integer.toString(execution.m_clientId));

        m_TWS.add( "symbol = " + contract.m_symbol);
        m_TWS.add( "secType = " + contract.m_secType);
        m_TWS.add( "expiry = " + contract.m_expiry);
        m_TWS.add( "strike = " + contract.m_strike);
        m_TWS.add( "right = " + contract.m_right);
        m_TWS.add( "contractExchange = " + contract.m_exchange);
        m_TWS.add( "currency = " + contract.m_currency);
        m_TWS.add( "localSymbol = " + contract.m_localSymbol);

        m_TWS.add( "execId = " + execution.m_execId);
        m_TWS.add( "time = " + execution.m_time);
        m_TWS.add( "acctNumber = " + execution.m_acctNumber);
        m_TWS.add( "executionExchange = " + execution.m_exchange);
        m_TWS.add( "side = " + execution.m_side);
        m_TWS.add( "shares = " + execution.m_shares);
        m_TWS.add( "price = " + execution.m_price);
        m_TWS.add( "permId = " + execution.m_permId);
        m_TWS.add( "liquidation = " + execution.m_liquidation);
        m_TWS.add( " ---- Execution Details end ----");
    }

    public void updateMktDepth( int tickerId, int position, int operation,
                    int side, double price, int size) {
        m_mktDepthDlg.updateMktDepth( tickerId, position, "", operation, side, price, size);
    }

    public void updateMktDepthL2( int tickerId, int position, String marketMaker,
                    int operation, int side, double price, int size) {
        m_mktDepthDlg.updateMktDepth( tickerId, position, marketMaker, operation, side, price, size);
    }

    public void nextValidId( int orderId) {
        // received next valid order id
        m_TWS.add("Next Valid Order ID: " + orderId) ;
        m_orderDlg.setIdAtLeast( orderId);
    }

    public void error( String str) {
        // received error
        m_errors.add( str);
    }

    public void error( int id, int errorCode, String errorMsg) {
        // received error
        String err = "";
        err += Integer.toString(id);
        err += " | ";
        err += Integer.toString(errorCode);
        err += " | ";
        err += errorMsg;
        m_errors.add( err);
        for (int ctr=0; ctr < faErrorCodes.length; ctr++) {
            faError |= (errorCode == faErrorCodes[ctr]);
        }
        if (errorCode == MktDepthDlg.MKT_DEPTH_DATA_RESET) {
            m_mktDepthDlg.reset();
        }
    }

    public void connectionClosed() {
        Main.inform( this, "Connection Closed");
    }

    public void updateAccountValue(String key, String value,
                                   String currency, String accountName) {
        m_acctDlg.updateAccountValue(key, value, currency, accountName);
    }

    public void updatePortfolio(Contract contract, int position, double marketPrice,
        double marketValue, double averageCost, double unrealizedPNL, double realizedPNL,
        String accountName) {
        m_acctDlg.updatePortfolio(contract, position, marketPrice, marketValue,
            averageCost, unrealizedPNL, realizedPNL, accountName);
    }

    public void updateAccountTime(String timeStamp) {
        m_acctDlg.updateAccountTime(timeStamp);
    }

    public void updateNewsBulletin( int msgId, int msgType, String message, String origExchange) {
        String msg;
        msg = " MsgId=" + msgId + " :: MsgType=" + msgType +  " :: Origin=" + origExchange + " :: Message=" + message;
        JOptionPane.showMessageDialog(this, msg, "IB News Bulletin", JOptionPane.INFORMATION_MESSAGE);
    }

    public void managedAccounts( String accountsList) {
        m_bIsFAAccount = true;
        m_FAAcctCodes = accountsList;

        m_TWS.add( "Connected : The list of managed accounts are : [" + accountsList + "]");
    }

    public void historicalData(int reqId, String date, double open, double high, double low, double close,
                                    double prevClose, int volume, double WAP, boolean hasGaps) {
        m_tickers.add( "id=" + reqId +
                        " date = " + date +
                        " open=" + open +
                        " high=" + high +
                        " low=" + low +
                        " close=" + close +
                        " prevClose=" + prevClose +
                        " volume=" + volume +
                        " WAP=" + WAP +
                        " hasGaps=" + hasGaps
                        );
    }

    public void scannerParameters(String xml) {
        displayXML("SCANNER PARAMETERS: ", xml);
    }

    void displayXML(String title, String xml) {
        m_TWS.add(title);
        m_TWS.addText(xml);
    }

    public void receiveFA(int faDataType, String xml) {
        displayXML("FA: " + EClientSocket.faMsgTypeName(faDataType), xml);
      switch (faDataType) {
        case EClientSocket.GROUPS:
          faGroupXML = xml ;
          break ;
        case EClientSocket.PROFILES:
          faProfilesXML = xml ;
          break ;
        case EClientSocket.ALIASES:
          faAliasesXML = xml ;
          break ;
      }

      if (!faError &&
          !(faGroupXML == null || faProfilesXML == null || faAliasesXML == null)) {
          FinancialAdvisorDlg dlg = new FinancialAdvisorDlg(this);
          dlg.receiveInitialXML(faGroupXML, faProfilesXML, faAliasesXML);
          dlg.show();

          if (!dlg.m_rc) {
            return;
          }

          m_client.replaceFA( EClientSocket.GROUPS, dlg.groupsXML );
          m_client.replaceFA( EClientSocket.PROFILES, dlg.profilesXML );
          m_client.replaceFA( EClientSocket.ALIASES, dlg.aliasesXML );

      }
    }

    private void copyExtendedOrderDetails( Order destOrder, Order srcOrder) {
        destOrder.m_tif = srcOrder.m_tif;
        destOrder.m_ocaGroup = srcOrder.m_ocaGroup;
        destOrder.m_ocaType = srcOrder.m_ocaType;
        destOrder.m_account = srcOrder.m_account;
        destOrder.m_openClose = srcOrder.m_openClose;
        destOrder.m_origin = srcOrder.m_origin;
        destOrder.m_orderRef = srcOrder.m_orderRef;
        destOrder.m_transmit = srcOrder.m_transmit;
        destOrder.m_parentId = srcOrder.m_parentId;
        destOrder.m_blockOrder = srcOrder.m_blockOrder;
        destOrder.m_sweepToFill = srcOrder.m_sweepToFill;
        destOrder.m_displaySize = srcOrder.m_displaySize;
        destOrder.m_triggerMethod = srcOrder.m_triggerMethod;
        destOrder.m_ignoreRth = srcOrder.m_ignoreRth;
        destOrder.m_rthOnly = srcOrder.m_rthOnly;
        destOrder.m_hidden = srcOrder.m_hidden;
        destOrder.m_discretionaryAmt = srcOrder.m_discretionaryAmt;
        destOrder.m_goodAfterTime = srcOrder.m_goodAfterTime;
        destOrder.m_shortSaleSlot = srcOrder.m_shortSaleSlot;
        destOrder.m_designatedLocation = srcOrder.m_designatedLocation;
        destOrder.m_ocaType = srcOrder.m_ocaType;
        destOrder.m_rthOnly = srcOrder.m_rthOnly;
        destOrder.m_rule80A = srcOrder.m_rule80A;
        destOrder.m_settlingFirm = srcOrder.m_settlingFirm;
        destOrder.m_allOrNone = srcOrder.m_allOrNone;
        destOrder.m_minQty = srcOrder.m_minQty;
        destOrder.m_percentOffset = srcOrder.m_percentOffset;
        destOrder.m_eTradeOnly = srcOrder.m_eTradeOnly;
        destOrder.m_firmQuoteOnly = srcOrder.m_firmQuoteOnly;
        destOrder.m_nbboPriceCap = srcOrder.m_nbboPriceCap;
        destOrder.m_auctionStrategy = srcOrder.m_auctionStrategy;
        destOrder.m_startingPrice = srcOrder.m_startingPrice;
        destOrder.m_stockRefPrice = srcOrder.m_stockRefPrice;
        destOrder.m_delta = srcOrder.m_delta;
        destOrder.m_stockRangeLower = srcOrder.m_stockRangeLower;
        destOrder.m_stockRangeUpper = srcOrder.m_stockRangeUpper;
        destOrder.m_overridePercentageConstraints = srcOrder.m_overridePercentageConstraints;
        destOrder.m_volatility = srcOrder.m_volatility;
        destOrder.m_volatilityType = srcOrder.m_volatilityType;
        destOrder.m_deltaNeutralOrderType = srcOrder.m_deltaNeutralOrderType;
        destOrder.m_deltaNeutralAuxPrice = srcOrder.m_deltaNeutralAuxPrice;
        destOrder.m_continuousUpdate = srcOrder.m_continuousUpdate;
        destOrder.m_referencePriceType = srcOrder.m_referencePriceType;
    }
}
