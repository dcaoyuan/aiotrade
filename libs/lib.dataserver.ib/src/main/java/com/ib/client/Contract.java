
package com.ib.client;

import java.util.*;

public class Contract implements Cloneable {
    public String m_symbol;
    public String m_secType;
    public String m_expiry;
    public double m_strike;
    public String m_right;
    public String m_multiplier;
    public String m_exchange;
    public String m_currency;
    public String m_localSymbol;
    public Vector m_comboLegs = new Vector();
    public String m_primaryExch;

    // BOND values
    public String m_cusip;
    public String m_ratings;
    public String m_descAppend;
    public String m_bondType;
    public String m_couponType;
    public boolean m_callable;
    public boolean m_putable;
    public double m_coupon;
    public boolean m_convertible;
    public String m_maturity;
    public String m_issueDate;

    public Contract() {
        m_strike = 0;
    }

    public Object clone() throws CloneNotSupportedException {
        Contract retval = (Contract)super.clone();
        retval.m_comboLegs = (Vector)retval.m_comboLegs.clone();
        return retval;
    }

    public Contract(String p_symbol, String p_secType, String p_expiry,
                    double p_strike, String p_right, String p_multiplier,
                    String p_exchange, String p_currency, String p_localSymbol,
                    Vector p_comboLegs, String p_primaryExch,
                    String p_cusip, String p_ratings, String p_descAppend,
                    String p_bondType, String p_couponType, boolean p_callable,
                    boolean p_putable, double p_coupon, boolean p_convertible,
                    String p_maturity, String p_issueDate) {
        m_symbol = p_symbol;
        m_secType = p_secType;
        m_expiry = p_expiry;
        m_strike = p_strike;
        m_right = p_right;
        m_multiplier = p_multiplier;
        m_exchange = p_exchange;
        m_currency = p_currency;
        m_localSymbol = p_localSymbol;
        m_comboLegs = p_comboLegs;
        m_primaryExch = p_primaryExch;
        m_cusip = p_cusip;
        m_ratings = p_ratings;
        m_descAppend = p_descAppend;
        m_bondType = p_bondType;
        m_couponType = p_couponType;
        m_callable = p_callable;
        m_putable = p_putable;
        m_coupon = p_coupon;
        m_convertible = p_convertible;
        m_maturity = p_maturity;
        m_issueDate = p_issueDate;
    }

    public boolean equals(Object p_other) {
        if (p_other == null ||
            ! (p_other instanceof Contract) ||
            m_comboLegs.size() != ( (Contract) p_other).m_comboLegs.size()) {
            return false;
        }
        else if (this == p_other) {
            return true;
        }

        Contract l_theOther = (Contract) p_other;
        boolean l_bContractEquals = false;

        String l_thisSecType = m_secType != null ? m_secType : "";
        String l_otherSecType = l_theOther.m_secType != null ? l_theOther.m_secType : "";

        if ( !l_thisSecType.equals(l_otherSecType)) {
            l_bContractEquals = false;
        } else {
            String l_thisSymbol = m_symbol != null ? m_symbol : "";
            String l_thisExchange = m_exchange != null ? m_exchange : "";
            String l_thisPrimaryExch = m_primaryExch != null ? m_primaryExch : "";
            String l_thisCurrency = m_currency != null ? m_currency : "";

            String l_otherSymbol = l_theOther.m_symbol != null ?
                l_theOther.m_symbol : "";
            String l_otherExchange = l_theOther.m_exchange != null ?
                l_theOther.m_exchange : "";
            String l_otherPrimaryExch = l_theOther.m_primaryExch != null ?
                l_theOther.m_primaryExch : "";
            String l_otherCurrency = l_theOther.m_currency != null ?
                l_theOther.m_currency : "";

            l_bContractEquals =
                l_thisSymbol.equals(l_otherSymbol) &&
                l_thisExchange.equals(l_otherExchange) &&
                l_thisPrimaryExch.equals(l_otherPrimaryExch) &&
                l_thisCurrency.equals(l_otherCurrency);

            if (l_bContractEquals) {
                if (l_thisSecType.equals("BOND")) {
                    l_bContractEquals = (m_putable == l_theOther.m_putable &&
                                         m_callable == l_theOther.m_callable &&
                                         m_convertible == l_theOther.m_convertible &&
                                         m_coupon == l_theOther.m_coupon);
                    if (l_bContractEquals) {
                        String l_thisCusip = m_cusip != null ? m_cusip : "";
                        String l_thisRatings = m_ratings != null ? m_ratings : "";
                        String l_thisDescAppend = m_descAppend != null ? m_descAppend : "";
                        String l_thisBondType = m_bondType != null ? m_bondType : "";
                        String l_thisCouponType = m_couponType != null ? m_couponType : "";
                        String l_thisMaturity = m_maturity != null ? m_maturity : "";
                        String l_thisIssueDate = m_issueDate != null ? m_issueDate : "";

                        String l_otherCusip = l_theOther.m_cusip != null ? l_theOther.m_cusip : "";
                        String l_otherRatings = l_theOther.m_ratings != null ? l_theOther.m_ratings : "";
                        String l_otherDescAppend = l_theOther.m_descAppend != null ? l_theOther.m_descAppend : "";
                        String l_otherBondType = l_theOther.m_bondType != null ? l_theOther.m_bondType : "";
                        String l_otherCouponType = l_theOther.m_couponType != null ? l_theOther.m_couponType : "";
                        String l_otherMaturity = l_theOther.m_maturity != null ? l_theOther.m_maturity : "";
                        String l_otherIssueDate = l_theOther.m_issueDate != null ? l_theOther.m_issueDate : "";
                        l_bContractEquals =
                            l_thisCusip.equals(l_otherCusip) &&
                            l_thisRatings.equals(l_otherRatings) &&
                            l_thisDescAppend.equals(l_otherDescAppend) &&
                            l_thisBondType.equals(l_otherBondType) &&
                            l_thisCouponType.equals(l_otherCouponType) &&
                            l_thisMaturity.equals(l_otherMaturity) &&
                            l_thisIssueDate.equals(l_otherIssueDate);
                    }
                }
                else {
                    String l_thisExpiry = m_expiry != null ? m_expiry : "";
                    String l_thisRight = m_right != null ? m_right : "";
                    String l_thisMultiplier = m_multiplier != null ? m_multiplier : "";
                    String l_thisLocalSymbol = m_localSymbol != null ? m_localSymbol : "";

                    String l_otherExpiry = l_theOther.m_expiry != null ?
                        l_theOther.m_expiry : "";
                    String l_otherRight = l_theOther.m_right != null ? l_theOther.m_right :
                        "";
                    String l_otherMultiplier = l_theOther.m_multiplier != null ?
                        l_theOther.m_multiplier : "";
                    String l_otherLocalSymbol = l_theOther.m_localSymbol != null ?
                        l_theOther.m_localSymbol : "";

                    l_bContractEquals =
                        l_thisExpiry.equals(l_otherExpiry) &&
                        m_strike == l_theOther.m_strike &&
                        l_thisRight.equals(l_otherRight) &&
                        l_thisMultiplier.equals(l_otherMultiplier) &&
                        l_thisLocalSymbol.equals(l_otherLocalSymbol);
                }
            }
        }

        if (l_bContractEquals && m_comboLegs.size() > 0) {
            // compare the combo legs
            boolean[] alreadyMatchedSecondLeg = new boolean[m_comboLegs.size()];
            for (int ctr1 = 0; ctr1 < m_comboLegs.size(); ctr1++) {
                ComboLeg l_thisComboLeg = (ComboLeg) m_comboLegs.get(ctr1);
                boolean l_bLegsEqual = false;
                for (int ctr2 = 0; ctr2 < l_theOther.m_comboLegs.size(); ctr2++) {
                    if (alreadyMatchedSecondLeg[ctr2]) {
                        continue;
                    }
                    if (l_thisComboLeg.equals(l_theOther.m_comboLegs.get(ctr2))) {
                        l_bLegsEqual = alreadyMatchedSecondLeg[ctr2] = true;
                        break;
                    }
                }
                if (!l_bLegsEqual) { // leg on first not matched by any previously unmatched leg on second
                    return false;
                }
            }
        }

        return l_bContractEquals;
    }
}
