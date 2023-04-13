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

import java.util.ResourceBundle
import javax.swing.JOptionPane
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.modules.ui.windows.ExplorerTopComponent
import org.aiotrade.modules.ui.dialog.ImportSymbolDialog
import org.openide.loaders.DataFolder
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction
import org.openide.windows.WindowManager

/**
 *
 * @author Caoyuan Deng
 */
class AddSymbolAction extends CallableSystemAction {
  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.actions.Bundle")
    
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          val symbolListTc = ExplorerTopComponent()
          symbolListTc.requestActive
                
          val selectedNodes = symbolListTc.getExplorerManager.getSelectedNodes
          var currentNode = if (selectedNodes.length > 0) {
            selectedNodes(0)
          } else null

          var currentFolder = if (currentNode != null) {
            currentNode.getLookup.lookup(classOf[DataFolder])
          } else null
                
          if (currentFolder == null) {
            /** add this stock in root folder */
            currentNode = symbolListTc.getExplorerManager.getRootContext
            currentFolder = currentNode.getLookup.lookup(classOf[DataFolder])
          }
                
          //- expand this node
          symbolListTc.getExplorerManager.setExploredContext(currentNode)
                
          // --- Now begin the dialog
                
          val quoteContract = new QuoteContract
          val pane = new ImportSymbolDialog(WindowManager.getDefault.getMainWindow, quoteContract, true)
          if (pane.showDialog != JOptionPane.OK_OPTION) {
            return
          }
                
          /** quoteContract may bring in more than one symbol, should process it here */
          for (symbol <- quoteContract.srcSymbol.split(",")) {
            val symbol1 = symbol.trim
                    
            /** dataSourceDescriptor may has been set to more than one symbols, process it here */
            quoteContract.srcSymbol = symbol1
                    
            SymbolNodes.createSymbolXmlFile(currentFolder, symbol1) foreach {
              // set attr to "open" to give a hint to SecurityNode.SymbolFolderChildren.creatNodes(Node)
              // so as to to open it automatically
              _.setAttribute("open", true)
            }
            
          }
        }
      })
        
  }
    
  def getName = {
    //"Add Symbol"
    val name = NbBundle.getMessage(this.getClass,"CTL_AddSymbolAction")
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


