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
package org.aiotrade.lib.charting.view.pane
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.widget.PathWidget



/**
 * We use a divisionPane instead of adding a mouse listener to other components,
 * because that we can easily define the dragged dy by compute the e.getY.
 * @see mouseDragged
 *
 * @author Caoyuan Deng
 */
class DivisionPane($view: ChartView, $datumPlane: DatumPlane) extends Pane($view, $datumPlane) {
  setOpaque(true)
  setRenderStrategy(RenderStrategy.NoneBuffer)
  setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR))
        
  private var divisionMouseAdapter = new DivisionMouseAdapter
  addMouseMotionListener(divisionMouseAdapter)
  addMouseListener(divisionMouseAdapter)

  /**
   * @Notice:
   * It seems horizontal or vertical line won't be painted by widger#renter because of not intersect.
   * But, I'll leave it as it, since I won't draw a real line now. We'll process border by chart pane itself.
   */
  override protected def plotPane {
    val pathWidget = addWidget(new PathWidget)
    pathWidget.setForeground(LookFeel().borderColor)
    val path = pathWidget.getPath
    path.reset
        
    path.moveTo(0, 0)
    path.lineTo(getWidth, 0)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    if (divisionMouseAdapter != null) {
      removeMouseListener(divisionMouseAdapter)
      removeMouseMotionListener(divisionMouseAdapter)
    }
        
    super.finalize
  }
    
  class DivisionMouseAdapter extends MouseAdapter with MouseMotionListener {
    private var readyToDrag = false
    private var startPos: Point = _
        
    override def mousePressed(e: MouseEvent) {
      startPos = e.getPoint
      readyToDrag = true
    }
        
    override def mouseDragged(e: MouseEvent) {
      if (!view.getParent.isInstanceOf[ChartViewContainer]) {
        return
      }
            
      val viewContainer = view.getParent.asInstanceOf[ChartViewContainer]
      if (readyToDrag) {
        // as this pane only 1 point height, the dy will always be e.getY
        val dy = e.getY
        viewContainer.adjustViewsHeight(dy)
      }
            
      startPos = e.getPoint
    }
        
    override def mouseReleased(e: MouseEvent) {
      readyToDrag = false
    }
        
    override def mouseMoved(e: MouseEvent) {
    }
  }
    
}
