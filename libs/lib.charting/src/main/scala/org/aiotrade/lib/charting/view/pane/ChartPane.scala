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
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.util.logging.Logger
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartValidityObserver
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.util.awt.AWTUtil

/**
 *
 * @author Caoyuan Deng
 */
class ChartPane(aview: ChartView) extends AbstractDatumPlane(aview) {
  private val log = Logger.getLogger(this.getClass.getName)
    
  private var colorTheme: LookFeel = _
    
  private var _isMouseEntered: Boolean = _
    
  private var _yMouse: Int = _
    
  private var _chartValid: Boolean = _
    
        
  setOpaque(false)
  setRenderStrategy(RenderStrategy.NoneBuffer)
        
  val mouseAdapter = new MyMouseAdapter
  addMouseListener(mouseAdapter)
  addMouseMotionListener(mouseAdapter)

  val myComponentListener = new ComponentListener {
    def componentHidden(e: ComponentEvent) {
      for (chart <- charts) {
        chart.reset
      }
      _chartValid = false
    }
            
    def componentMoved(e: ComponentEvent) {
    }
            
    def componentResized(e: ComponentEvent) {
      _chartValid = false
    }
            
    def componentShown(e: ComponentEvent) {
    }
  }
  addComponentListener(myComponentListener)
        
  view.controller.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case _: ChartingController =>
          _chartValid = false
          isGeometryValid = false
      }
    })
        
  view.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case _: ChartView =>
          _chartValid = false
          isGeometryValid = false
      }
    })
    
  protected def isChartValid: Boolean = {
    _chartValid && isGeometryValid
  }
    
  override protected def plotPane {
    colorTheme = LookFeel()
  }

  def isMouseEntered = _isMouseEntered

  def yMouse: Int = _yMouse
  def yMouse_=(yMouse: Int) {
    this._yMouse = yMouse
  }
    

  @throws(classOf[Throwable])
  override protected def finalize {
    view.controller.removeObserversOf(this)
    view.removeObserversOf(this)
        
    AWTUtil.removeAllAWTListenersOf(this)

    super.finalize
  }
    
  class MyMouseAdapter extends MouseAdapter with MouseMotionListener {
        
    var oldBMouse = -Integer.MAX_VALUE
    var oldYMouse = -Integer.MAX_VALUE
        
    override def mousePressed(e: MouseEvent) {
      if (e.isPopupTrigger) return // isPopupTrigger is the event of mousePressed and mouseReleased instead of mouseClicked
            
      if (!view.isInteractive) {
        /**
         * we don't want the click changes the refer cursor position and
         * selects a chart etc.
         */
        return
      }
            
      if (view.isInstanceOf[WithDrawingPane]) {
        val drawing = view.asInstanceOf[WithDrawingPane].selectedDrawing
        if (drawing != null && drawing.isInDrawing) {
          return
        }
      }
            
      if (e.isControlDown) {
        if (!(view.getParent.isInstanceOf[ChartViewContainer])) {
          return
        }
        val viewContainer = view.getParent.asInstanceOf[ChartViewContainer]
        val selectedChart = viewContainer.selectedChart
        val theChart = chartAt(e.getX, e.getY)
        if (theChart != null) {
          if (theChart == selectedChart) {
            /** deselect it */
            viewContainer.selectedChart = null
          } else {
            viewContainer.selectedChart = theChart
          }
        } else {
          viewContainer.selectedChart = null
        }
      } else {
        /** set refer cursor */
        val y = e.getY
        val b = bx(e.getX)
        if (y >= view.TITLE_HEIGHT_PER_LINE && y <= getHeight - (if (view.xControlPane == null) 0 else view.CONTROL_HEIGHT) &&
            b >= 1 && b <= nBars) {
          val position = rb(b)
          view.controller.setReferCursorByRow(position, true)
        }
      }
    }

    override def mouseMoved(e: MouseEvent) {
      val y = e.getY

      if (y >= view.TITLE_HEIGHT_PER_LINE && y <= getHeight - (if (view.xControlPane == null) 0 else view.CONTROL_HEIGHT)) {
        _isMouseEntered = true
        view.controller.isMouseEnteredAnyChartPane = true
      } else {
        _isMouseEntered = false
        view.controller.isMouseEnteredAnyChartPane = false
      }
            
      val b = bx(e.getX)
            
      /** mouse position really changed? */
      if (oldBMouse == b && oldYMouse == y) {
        return
      }
            
      if (b >= 1 && b <= nBars) {
        yMouse = y
        val row = rb(b)
        view.controller.setMouseCursorByRow(row)
      }
            
      oldBMouse = b
      oldYMouse = y
    }
        
    override def mouseEntered(e: MouseEvent) {
      _isMouseEntered = true
      view.controller.isMouseEnteredAnyChartPane = true
    }
        
    override def mouseExited(e: MouseEvent) {
      _isMouseEntered = false
      view.controller.isMouseEnteredAnyChartPane = false
    }
        
    override def mouseDragged(e: MouseEvent) {
      mouseMoved(e)
      //view.getController().setMouseEnteredAnyChartPane(false);
    }
  }

}
