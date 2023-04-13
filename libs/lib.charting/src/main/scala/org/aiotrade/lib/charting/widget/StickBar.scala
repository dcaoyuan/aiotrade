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
 * @version 1.0, November 29, 2006, 5:27 PM
 * @since   1.0.4
 */
class StickBar extends PathWidget {
  
  final class Model extends WidgetModel {
    var xCenter: Double = _
    var y1: Double = _
    var y2: Double = _
    var width: Double = _
    var thin: Boolean = _
    var isFilled: Boolean = _
        
    def set(xCenter: Double, y1: Double, y2: Double, width: Double, thin: Boolean, isFilled: Boolean) {
      this.xCenter = xCenter
      this.y1 = y1
      this.y2 = y2
      this.width = width
      this.thin = thin
      this.isFilled = isFilled
    }
  }

  type M = Model
    
  override protected def createModel: Model = new Model

  override protected def plotWidget {
    val m = model
    val path = getPath
    path.reset
    
    val xRadius = (if (m.width < 2) 0 else (m.width - 2) / 2)
        
    if (m.thin || m.width <= 2) {
      path.moveTo(m.xCenter, m.y1)
      path.lineTo(m.xCenter, m.y2)
    } else {
      path.moveTo(m.xCenter - xRadius, m.y1)
      path.lineTo(m.xCenter - xRadius, m.y2)
      path.lineTo(m.xCenter + xRadius, m.y2)
      path.lineTo(m.xCenter + xRadius, m.y1)
      path.closePath

      isFilled = m.isFilled
//      if (m.isFilled) {
//        var i = 1
//        while (i < m.width - 2) {
//          path.moveTo(m.xCenter - xRadius + i, m.y1)
//          path.lineTo(m.xCenter - xRadius + i, m.y2)
//          i += 1
//        }
//      }
            
    }
  }
    
}
