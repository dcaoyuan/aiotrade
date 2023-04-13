
package com.ib.client;

public class ContractDetails {
    public Contract	m_summary;
    public String 	m_marketName;
    public String 	m_tradingClass;
    public int 		m_conid;
    public double 	m_minTick;
    public String 	m_multiplier;
    public int      m_priceMagnifier;
    public String 	m_orderTypes;
    public String 	m_validExchanges;

    public ContractDetails() {
        m_summary = new Contract();
        m_conid = 0;
        m_minTick = 0;
    }

    public ContractDetails( Contract p_summary, String p_marketName, String p_tradingClass,
    		int p_conid, double p_minTick, String p_multiplier, String p_orderTypes,
    		String p_validExchanges) {
        m_summary = p_summary;
    	m_marketName = p_marketName;
    	m_tradingClass = p_tradingClass;
    	m_conid = p_conid;
    	m_minTick = p_minTick;
    	m_multiplier = p_multiplier;
    	m_orderTypes = p_orderTypes;
    	m_validExchanges = p_validExchanges;
    }
}
