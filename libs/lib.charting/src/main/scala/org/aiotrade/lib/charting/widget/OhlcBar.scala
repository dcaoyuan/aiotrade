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
package org.aiotrade.lib.charting.widget


/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 10:13 PM
 * @since   1.0.4
 */
class OhlcBar extends PathWidget {
  final class Model extends WidgetModel {
    var xCenter: Double = _
    var yOpen:   Double = _
    var yHigh:   Double = _
    var yLow:    Double = _
    var yClose:  Double = _
    var width:   Double = _
        
    def set(xCenter: Double, yOpen: Double, yHigh: Double, yLow: Double, yClose: Double, width: Double) {
      this.xCenter = xCenter
      this.yOpen = yOpen
      this.yHigh = yHigh
      this.yLow = yLow
      this.yClose = yClose
      this.width = width
    }
  }

  type M = Model
    
  override protected def createModel: Model = new Model
    
  /**
   *         12341234
   *          |
   *         -+   |
   *          |   +-
   *          +- -+
   *              |
   *
   *          ^   ^
   *          |___|___ barCenter
   */
  override protected def plotWidget {
    val m = model
    val path = getPath
    path.reset
        
    /** why - 2 ? 1 for centre, 1 for space */
    val xRadius = if (m.width < 2) 0 else (m.width - 2) / 2
        
    if (m.width <= 2) {
      path.moveTo(m.xCenter, m.yHigh)
      path.lineTo(m.xCenter, m.yLow)
    } else {
      path.moveTo(m.xCenter, m.yHigh)
      path.lineTo(m.xCenter, m.yLow)
            
      path.moveTo(m.xCenter - xRadius, m.yOpen)
      path.lineTo(m.xCenter, m.yOpen)
            
      path.moveTo(m.xCenter, m.yClose)
      path.lineTo(m.xCenter + xRadius, m.yClose)
    }
        
  }
    
}
