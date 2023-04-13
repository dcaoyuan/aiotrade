/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction

/**
 *
 * @author Caoyuan Deng
 */
class SwichAnalysisReaTimeAction extends CallableSystemAction {

  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable {
          def run {
            for (tc <- AnalysisChartTopComponent.selected;
                 sec = tc.sec;
                 content = sec.content;
                 quoteContract <- content.lookupActiveDescriptor(classOf[QuoteContract])
            ) {
              val toFreq = if (tc.viewContainer.isInstanceOf[RealTimeChartViewContainer]) {
                TFreq.DAILY
              } else {
                TFreq.ONE_SEC
              }
              quoteContract.freq = toFreq
              val newTc = AnalysisChartTopComponent(sec)
              newTc.requestActive
            }
          }
        })
    } catch {case ex: Exception =>}
  }

  def getName: String = NbBundle.getMessage(this.getClass, "CTL_SwichAnalysisReaTimeAction")

  def getHelpCtx: HelpCtx = HelpCtx.DEFAULT_HELP

  override protected def iconResource: String = "org/aiotrade/modules/ui/resources/realtime.jpg"

  override protected def asynchronous: Boolean = false
}




