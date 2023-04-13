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

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.charting.view.pane.XControlPane
import org.aiotrade.lib.util.swing.GBC

/**
 *
 * @author Caoyuan Deng
 */
class PopupIndicatorChartView(acontroller: ChartingController,
                              amainSer: TSer,
                              empty: Boolean
) extends IndicatorChartView(acontroller, amainSer, empty) {

  def this(controller: ChartingController, mainSer: TSer) = this(controller, mainSer, false)
  def this() = this(null, null, true)
    
  override protected def initComponents {
    xControlPane = new XControlPane(this, mainChartPane)
    xControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    /** begin to set the layout: */
        
    setLayout(new GridBagLayout)
        
    /**
     * @NOTICE be ware of the components added order:
     * 1. add xControlPane, it will partly cover glassPane in SOUTH,
     * 2. add glassPane, it will exactly cover mainLayeredPane
     * 3. add mainLayeredPane.
     *
     * After that, xControlPane can accept its self mouse events, and so do
     * glassPane except the SOUTH part covered by xControlPane.
     *
     * And glassPane will forward mouse events to whom it covered.
     * @see GlassPane#processMouseEvent(MouseEvent) and
     *      GlassPane#processMouseMotionEvent(MouseEvent)
     */

    add(xControlPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
        
    add(glassPane,  GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    add(mainLayeredPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    /**
     * add the axisYPane in the same grid as yControlPane then, it will be
     * covered by yControlPane partly in SOUTH
     */
    add(axisYPane, GBC(1, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(0, 100))
        
    /** add axisXPane and dividentPane across 2 gridwidth horizontally, */
    add(axisXPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
  }
    
}



