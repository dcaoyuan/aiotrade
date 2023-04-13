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
package org.aiotrade.lib.charting.view

import javax.swing.JLayeredPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.charting.view.pane.DrawingPane
import scala.collection.mutable

/**
 * This class help to implement a default drawingPart support.
 *
 *
 *
 *
 * @author Caoyuan Deng
 * @TIPS As Java does not support multi-inheriting, here shows a good solution, example:
 * QuoteChartView, which has been a sub-class of ChartView, but want some
 * Default drawing feature too. It implement WithDrawingPane interface via this
 * cookie. Actually, that is something like inhertied from ChartView and
 * WithDrawingPaneHelper.
 */
class WithDrawingPaneHelper(owner: ChartView) extends WithDrawingPane {
  val descriptorToDrawing = mutable.Map[DrawingDescriptor, DrawingPane]()
  private var _selectedDrawing: DrawingPane = _
    
  def selectedDrawing: DrawingPane = _selectedDrawing
    
  def selectedDrawing_=(drawing: DrawingPane) {
    _selectedDrawing = drawing
  }
    
  def findDrawingDescriptor(drawing: DrawingPane): DrawingDescriptor = {
    for (descriptor <- descriptorToDrawing.keySet) {
      val foundDrawing = descriptorToDrawing.get(descriptor)
      if ( foundDrawing != null && foundDrawing.equals(drawing)) {
        return descriptor
      }
    }
    null;
  }
    
  def addDrawing(descriptor: DrawingDescriptor, drawing: DrawingPane) {
    if (descriptorToDrawing.contains(descriptor)) {
      /** if this has been in drawings, don't add more */
      selectedDrawing = drawing
    } else {
      descriptorToDrawing.put(descriptor, drawing)
            
      owner.mainLayeredPane.add(drawing, JLayeredPane.DEFAULT_LAYER)
      drawing.setVisible(false)
      owner.mainLayeredPane.moveToBack(drawing)
            
      selectedDrawing = drawing
    }
  }
    
  def deleteDrawing(descriptor: DrawingDescriptor) {
    descriptorToDrawing.get(descriptor) foreach {drawing =>
      owner.mainLayeredPane.remove(drawing)
      if (_selectedDrawing != null && _selectedDrawing.equals(drawing)) {
        selectedDrawing = null
        owner.controller.isCursorCrossLineVisible = true
      }
      owner.controller.updateViews
    }
    descriptorToDrawing.remove(descriptor)
  }
    
  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
}




