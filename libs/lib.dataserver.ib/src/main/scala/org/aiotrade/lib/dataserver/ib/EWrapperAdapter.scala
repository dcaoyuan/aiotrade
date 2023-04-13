/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.dataserver.ib;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;

/**
 * 
 * @author Caoyuan Deng
 */
class EWrapperAdapter extends EWrapper {
    
  def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {}
  def tickSize(tickerId: Int, field: Int, size: Int) {}
  def tickOptionComputation(tickerId: Int, field: Int, impliedVolatility: Double, delta: Double) {}
  def orderStatus(orderId: Int, status: String, filled: Int, remaining: Int,
                  avgFillPrice: Double, permId: Int, parentId: Int, lastFillPrice: Double,
                  clientId: Int) {}
  def openOrder(orderId: Int, contract: Contract, order: Order) {}
  def error(str: String) {}
  def connectionClosed {}
  def updateAccountValue(key: String, value: String, currency: String, accountName: String) {}
  def updatePortfolio(contract: Contract, position: Int, marketPrice: Double, marketValue: Double,
                      averageCost: Double, unrealizedPNL: Double, realizedPNL: Double, accountName: String) {}
  def updateAccountTime(timeStamp: String) {}
  def nextValidId(orderId: Int) {}
  def contractDetails(contractDetails: ContractDetails) {}
  def bondContractDetails(contractDetails: ContractDetails) {}
  def execDetails(orderId: Int, contract: Contract, execution: Execution) {}
  def error(id: Int, errorCode: Int, errorMsg: String) {}
  def updateMktDepth(tickerId: Int, position: Int, operation: Int, side: Int, price: Double, size: Int) {}
  def updateMktDepthL2(tickerId: Int, position: Int, marketMaker: String, operation: Int, side: Int, price: Double, size: Int) {}
  def updateNewsBulletin(msgId: Int, msgType: Int, message: String, origExchange: String) {}
  def managedAccounts(accountsList: String) {}
  def receiveFA(faDataType: Int, xml: String) {}
  def historicalData(reqId: Int, date: String, open: Double, high: Double, low: Double, close: Double, prevClose: Double,
                     volume: Int, WAP: Double, hasGaps: Boolean) {}
  def scannerParameters(xml: String) {}
  def scannerData(reqId: Int, rank: Int, contractDetails: ContractDetails, distance: String, benchmark: String, projection: String) {}
}

