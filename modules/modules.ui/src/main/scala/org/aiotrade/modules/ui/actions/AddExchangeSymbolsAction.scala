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
package org.aiotrade.modules.ui.actions

import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.modules.ui.windows.ExplorerTopComponent
import org.openide.loaders.DataFolder
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction

/**
 *
 * @author Caoyuan Deng
 */
class AddExchangeSymbolsAction extends CallableSystemAction {
    
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          val explorerTc = ExplorerTopComponent()
          explorerTc.requestActive

          val rootNode = explorerTc.getExplorerManager.getRootContext
          val rootFolder = rootNode.getLookup.lookup(classOf[DataFolder])

          // expand root node
          explorerTc.getExplorerManager.setExploredContext(rootNode)
                
          // add symbols to exchange folder
          for (exchange <- Exchange.allExchanges;
               exchangeFolder = DataFolder.create(rootFolder, exchange.code);
               symbol <- Exchange.symbolsOf(exchange)
          ) {
            SymbolNodes.createSymbolXmlFile(exchangeFolder, symbol)
          }
        }
      })
        
  }

  def getName = {
    //"Add Exchange Symbols"
    val name = NbBundle.getMessage(this.getClass,"CTL_AddExchangeSymbolsAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/newSymbol.gif"
  }
    
  override protected def asynchronous: Boolean = {
    false
  }
    
    
}


