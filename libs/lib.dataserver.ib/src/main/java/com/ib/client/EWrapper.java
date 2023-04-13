
package com.ib.client;

public interface EWrapper {

    ///////////////////////////////////////////////////////////////////////
    // Interface methods
    ///////////////////////////////////////////////////////////////////////
    void tickPrice( int tickerId, int field, double price, int canAutoExecute);
    void tickSize( int tickerId, int field, int size);
    void tickOptionComputation( int tickerId, int field, double impliedVolatility, double delta);
    void orderStatus( int orderId, String status, int filled, int remaining,
            double avgFillPrice, int permId, int parentId, double lastFillPrice,
            int clientId);
    void openOrder( int orderId, Contract contract, Order order);
    void error( String str);
    void connectionClosed();
    void updateAccountValue(String key, String value, String currency, String accountName);
    void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue,
            double averageCost, double unrealizedPNL, double realizedPNL, String accountName);
    void updateAccountTime(String timeStamp);
    void nextValidId( int orderId);
    void contractDetails(ContractDetails contractDetails);
    void bondContractDetails(ContractDetails contractDetails);
    void execDetails( int orderId, Contract contract, Execution execution);
    void error(int id, int errorCode, String errorMsg);
    void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size);
    void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation, int side, double price, int size);
    void updateNewsBulletin( int msgId, int msgType, String message, String origExchange);
    void managedAccounts( String accountsList);
    void receiveFA(int faDataType, String xml);
    void historicalData(int reqId, String date, double open, double high, double low,double close, double prevClose,
                       int volume, double WAP, boolean hasGaps);
    void scannerParameters(String xml);
    void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection);
}
