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
package org.aiotrade.lib.charting.chart.handledchart

import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import org.aiotrade.lib.charting.chart.segment.Handle
import org.aiotrade.lib.charting.chart.segment.ValuePoint
import org.aiotrade.lib.charting.view.pane.DatumPlane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.pane.DrawingPane
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.util.swing.action.EditAction
import scala.collection.mutable.ArrayBuffer


/**
 *
 * @author Caoyuan Deng
 */
object AbstractHandledChart {
  private val HANDLE_CURSOR  = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  private val MOVE_CURSOR    = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)

  /** @NOTICE should define this to Integer.MAX_VALUE instead of any other values */
  val VARIABLE_NUMBER_OF_HANDLES = Integer.MAX_VALUE
}

abstract class AbstractHandledChart(drawing: DrawingPane, points: ArrayBuffer[ValuePoint]) extends HandledChart {
  import AbstractHandledChart._
    
  private var drawingPane: DrawingPane = _
  private var paneMouseAdapterForDrawingAdded = false
    
  private var datumPlane: DatumPlane = _
  private var chart: C = _
    
  /**
   * define final members, so we can be sure that they won't be pointed to
   * another object, even in case of being got by others via public or
   * protected method
   */
  private val currHandles = new ArrayBuffer[Handle]
  private val prevHandles = new ArrayBuffer[Handle]
    
  /** For moving chart: the valuePoint and handls when mouse is pressed before drag */
  private val currHandlesWhenMousePressed = new ArrayBuffer[Handle]
  /**
   * define mousePressedPoint as final to force using copy(..) to set its value
   */
  private val mousePressedPoint = new ValuePoint
    
  private val allCurrHandlesPathBuf = new GeneralPath
    
  private var nHandles = 0
  private var selectedHandleIdx = 0
    
  private var firstStretch = true
  private var accomplished = false
  private var anchored = false
  private var activated = false
  private var readyToDrag = false
    
  private var paneMouseAdapter: PaneMouseAdapter = _
    
  private var cursor: Cursor = _
    
  private val pointBuf = new ValuePoint
  private val handlePointsBuf = new ArrayBuffer[ValuePoint]
    

  if (points == null) {
    this.chart = init
    this.chart.depth = Pane.DEPTH_DRAWING

    if (drawing != null) {
      attachDrawingPane(drawing)
    }
  } else {
    init(drawing, points)
  }
    
  def this(drawing: DrawingPane) = this(drawing, null)

  /**
   * @NOTICE
   * Should define a no args constructor explicitly for class.newInstance()
   */
  def this() = this(null)

  protected def init: C
    
  /**
   * init with known points
   */
  def init(drawing: DrawingPane, points: ArrayBuffer[ValuePoint]) {
    assert(points != null, "this is for points known HandledChart!")
        
    attachDrawingPane(drawing)
    val size = points.size
    setNHandles(size)

    var i = 0
    while (i < size) {
      currHandles += new Hdl
      prevHandles += new Hdl
      currHandlesWhenMousePressed += new Hdl
            
      /** assign currentHandles' points to points */
      currHandles(i).copyPoint(points(i))
      i += 1
    }
        
    accomplished = true
        
    /** set chart' arg according to current handles */
    setChartModel(currHandles)
        
    /** now the chart's arg has been set, and ready to be put to drawing pane */
    drawing.putChart(chart)
    drawing.view.mainLayeredPane.moveToBack(drawing)
  }
    
  def attachDrawingPane(drawing: DrawingPane) {
    if (this.drawingPane == null || this.drawingPane != drawing) {
      this.drawingPane = drawing
      this.datumPlane = drawing.datumPlane
            
      /** should avoid listener being added more than once */
      if (!paneMouseAdapterForDrawingAdded) {
        addMouseAdapterToPane
      }
            
      assert(chart != null, "chart instance should has been created!")
      chart.set(datumPlane, datumPlane.baseSer, Pane.DEPTH_DRAWING)
    }
  }
    
  private def addMouseAdapterToPane {
    if (drawingPane != null) {
      paneMouseAdapter = new PaneMouseAdapter
      drawingPane.addMouseListener(paneMouseAdapter)
      drawingPane.addMouseMotionListener(paneMouseAdapter)
            
      paneMouseAdapterForDrawingAdded = true
    }
  }
    
  protected def setNHandles(nHandles: Int) {
    this.nHandles = nHandles
  }
    
  def removeMouseAdapterOnPane {
    if (drawingPane != null) {
      drawingPane.removeMouseListener(paneMouseAdapter)
      drawingPane.removeMouseMotionListener(paneMouseAdapter)
      paneMouseAdapter = null
            
      paneMouseAdapterForDrawingAdded = false
    }
  }
    
  /**
   *
   *
   * @return <code>true</code> if accomplished after this anchor,
   *         <code>false</code> if not yet.
   */
  private def anchorHandle(point: ValuePoint): Boolean = {
        
    if (currHandles.size == 0) {
      if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
        /** this is a nHandles variable chart, create first handle */
        currHandles += new Hdl
        prevHandles += new Hdl
        currHandlesWhenMousePressed += new Hdl
      } else {
        /** this is a nHandles pre-defined chart, create all of the handles */
        var i = 0
        while (i < nHandles) {
          currHandles += new Hdl
          prevHandles += new Hdl
          currHandlesWhenMousePressed += new Hdl
          i += 1
        }
      }
    }
        
    currHandles(selectedHandleIdx).copyPoint(point);
        
    if (!accomplished) {
      /** make handles that not yet anchored having the same position as selectedHandle */
      val n = currHandles.size
      var i = selectedHandleIdx + 1
      while (i < n) {
        currHandles(i).copyPoint(point)
        i += 1
      }
    }
        
    backupCurrentHandlesToPreviousHandles
        
    if (selectedHandleIdx < nHandles - 1) {
      anchored = true
            
      /** select next handle */
      selectedHandleIdx += 1
            
      /** create next handle if not created yet */
      if (currHandles.size - 1 < selectedHandleIdx) {
        currHandles += new Hdl(point)
        prevHandles += new Hdl(point)
        currHandlesWhenMousePressed += new Hdl
      }
    } else {
      /** if only one handle, should let it be drawn at once */
      if (nHandles == 1) {
        stretchHandle(point)
      }
            
      anchored = false
      accomplished = true
      selectedHandleIdx = -1
    }
        
    return accomplished;
  }
    
    
  private def stretchHandle(point: ValuePoint) {
        
    backupCurrentHandlesToPreviousHandles
        
    /** set selectedHandle's new position */
    currHandles(selectedHandleIdx).copyPoint(point)
        
    if (!accomplished) {
      var i = selectedHandleIdx + 1
      val n = currHandles.size
      while (i < n) {
        currHandles(i).copyPoint(currHandles(selectedHandleIdx).getPoint)
        i += 1
      }
    }
        
        
    val g = drawingPane.getGraphics
    if (g != null) {
      try {
        g.setXORMode(drawingPane.getBackground)
                
        if (firstStretch) {
          firstStretch = false
        } else {
          /** erase previous drawing */
          renderPrevious(g)
          prevHandles(selectedHandleIdx).render(g)
        }
        /** current new drawing */
        renderCurrent(g)
        currHandles(selectedHandleIdx).render(g)
                
        /** restore to paintMode */
        g.setPaintMode
                
        if (!accomplished) {
          var i = 0
          while (i < selectedHandleIdx) {
            currHandles(i).render(g)
            i += 1
          }
        } else {
          val n = currHandles.size
          var i = 0
          while (i < n) {
            if (i != selectedHandleIdx) {
              currHandles(i).render(g)
            }
            i += 1
          }
        }
      } finally {
        g.dispose
      }
    }
  }
    
  private def moveChart(mouseDraggedPoint: ValuePoint) {
        
    backupCurrentHandlesToPreviousHandles
        
    /**
     * should compute bar moved instead of time moved, because when shows
     * in trading date mode, time moved may not be located at a trading day
     */
    val barMoved = datumPlane.bt(mouseDraggedPoint.t) - datumPlane.bt(mousePressedPoint.t)
    val vMoved = mouseDraggedPoint.v - mousePressedPoint.v
        
    val newPoint = new ValuePoint
    val n = currHandles.size
    var i = 0
    while (i < n) {
      val oldPoint = currHandlesWhenMousePressed(i).getPoint
            
      /** compute newTime, process bar fisrt, then transfer to time */
      val oldBar  = datumPlane.bt(oldPoint.t)
      val newBar  = oldBar + barMoved;
      val newTime = datumPlane.tb(newBar);
            
      /** compute newValue */
      val newValue = oldPoint.v + vMoved
            
      /**
       * @NOTICE
       * do not use getPoint().set(newTime, newValue) to change point member,
       * because we need handle to recompute position. use copyPoint().
       */
      newPoint.set(newTime, newValue)
      currHandles(i).copyPoint(newPoint)

      i += 1
    }
        
    val g = drawingPane.getGraphics
    if (g != null) {
      try {
        g.setXORMode(drawingPane.getBackground)
                
        /** erase previous drawing */
        renderPrevious(g)
        renderHandles(g, prevHandles)
                
        /** current new drawing */
        renderCurrent(g)
        renderHandles(g, currHandles)
      } finally {
        g.dispose
      }
    }
        
  }
    
  private def backupCurrentHandlesToPreviousHandles {
    var i = 0
    val n = currHandles.size
    while (i < n) {
      prevHandles(i).copyPoint(currHandles(i).getPoint)
      i += 1
    }
  }
    
  private def renderHandles(g: Graphics , handles: ArrayBuffer[Handle]) {
    for (handle <- handles) {
      handle.render(g)
    }
  }
    
  def activate {
    this.activated = true;
  }
    
  def passivate {
    this.activated = false;
  }
    
  def isActivated: Boolean = {
    activated;
  }
    
  def isAccomplished: Boolean = {
    accomplished;
  }
    
  private def isReadyToDrag: Boolean = {
    readyToDrag
  }
    
  def getCurrentHandlesPoints: ArrayBuffer[ValuePoint] = {
    handlesPoints(currHandles)
  }
    
  protected def handlesPoints(handles: ArrayBuffer[Handle]): ArrayBuffer[ValuePoint] = {
    handlePointsBuf.clear
    var i = 0
    val n = handles.size
    while (i < n) {
      handlePointsBuf += handles(i).getPoint
      i += 1
    }
        
    handlePointsBuf
  }
    
  private def setReadyToDrag(b: Boolean) {
    readyToDrag = b
  }
    
  def getChart: C = {
    chart
  }
    
  def getAllCurrentHandlesPath: GeneralPath = {
    allCurrHandlesPathBuf.reset
    for (handle <- currHandles) {
      allCurrHandlesPathBuf.append(handle.getPath, false)
    }
        
    allCurrHandlesPathBuf;
  }
    
  private def getHandleAt(x: Int, y: Int): Handle = {
    for (handle <- currHandles) {
      if (handle.contains(x, y)) {
        return handle
      }
    }
        
    null
  }
    
  def getCursor: Cursor = {
    cursor
  }
    
  /**
   * @NOTCIE
   * use this method carefullly:
   * as it always return the same instance, should copy its value instead of
   * set to it or '=' it.
   */
  private def p(e: MouseEvent): ValuePoint = {
    pointBuf.set(datumPlane.tx(e.getX), datumPlane.vy(e.getY))
        
    pointBuf
  }
    
  private def renderPrevious(g: Graphics) {
    setChartModelAndRenderChart(g, prevHandles)
  }
    
  private def renderCurrent(g: Graphics) {
    setChartModelAndRenderChart(g, currHandles)
  }
    
  private def setChartModelAndRenderChart(g: Graphics, handles: ArrayBuffer[Handle]) {
    /** 1. set chart's model according to the handles */
    setChartModel(handles)
        
    /** 2. plot chart, now chart is ready for drawing */
    chart.plot
        
    /** 3. draw chart using g */
    chart.render(g)
  }
    
  /**
   * set the chart's model according to the handles.
   *
   * @param handles the list of handles to be used to set the model
   */
  protected def setChartModel(handles: ArrayBuffer[Handle])
    
  final def compare(another: HandledChart): Int = {
    if (this.toString.equalsIgnoreCase(another.toString)) {
      if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
    } else {
      this.toString.compare(another.toString)
    }
  }
    
  def createNewInstance: HandledChart = {
    try {
      this.getClass.newInstance.asInstanceOf[HandledChart]
    } catch {case ex: Exception =>ex.printStackTrace; null}
  }
    
  /**
   * mouse adapter for handling drawing.
   *
   * @NOTICE it will be added to drawingPane instead of HandledChart
   *
   * @NOTICE Since too many process concerning with HandleChart itself, if we define
   * mouse adapter on DrawingPane.java instead of here, we shall open a couple
   * of public method for HandledChart, and transfer the mouse event to here,
   * makeing almost a MouseAdapter for this class. So, we define MouseAdapter
   * here neck and crop.
   *
   * As each HandledChart own a private this listener , which will be added to
   * drawingPane when attachDrawingPane. So, drawingPane may has more than one
   * listener(s). So be careful to define the mouse behaves minding other
   * HandledChart(s).
   */
  class PaneMouseAdapter extends MouseAdapter with MouseMotionListener {
    override def mouseClicked(e: MouseEvent) {
      /** sinlge-clicked ? go on drawing, or, check my selection status */
      if (e.getClickCount == 1) {
                
        /** go on drawing ? */
        if (isActivated && !isAccomplished) {
          val accomplishedNow = anchorHandle(p(e))
          if (accomplishedNow) {
            drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this)
          }
          /** always set this is selected in this case: */
          getChart.isSelected = true
        }
        /** else, check my selection status */
        else {
          if (chart.hits(e.getPoint)) {
            if (getChart.isSelected) {
              getChart.lookupActionAt(classOf[EditAction], e.getPoint) foreach {action =>
                /** as the glassPane is always in the front, so add it there */
                action.anchorEditor(drawingPane.view.glassPane)
                action.execute
              }
            }
                        
            getChart.isSelected = true
            /**
             * I was just selected only, don't call activate() here, let drawingPane
             * to decide if also activate me.
             */
          } else {
            getChart.isSelected = false
            /**
             * I was just deselected only, don't call passivate() here, let drawingPane
             * to decide if also passivate me.
             */
          }
        }
                
      }
      /** double clicked, process chart whose nHandles is variable */
      else {
                
        if (!isAccomplished) {
          if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
            anchored = false;
            accomplished = true;
            selectedHandleIdx = -1;
                        
            drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this);
          }
        }
      }
    }
        
    override def mousePressed(e: MouseEvent) {
      if (isReadyToDrag) {
        mousePressedPoint.copy(p(e))
        /** record handles when mouse pressed, for moveChart() */
        var i = 0
        val n = currHandles.size
        while (i < n) {
          currHandlesWhenMousePressed(i).copyPoint(currHandles(i).getPoint)
          i += 1
        }
      }
            
      /** @TODO */
      //            if (isAccomplished() && isActive()) {
      //                if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
      //                    /** edit(add/delete handle) chart whose nHandles is variable */
      //                    if (e.isControlDown()) {
      //                        Handle theHandle = handleAt(e.getX(), e.getY());
      //                        if (theHandle != null) {
      //                            /** delete handle */
      //                            int idx = currentHandles.indexOf(theHandle);
      //                            if (idx > 0) {
      //                                currentHandles.remove(idx);
      //                                previousHandles.remove(idx);
      //                                currentHandlesWhenMousePressed.remove(idx);
      //                            }
      //                        } else {
      //                            /** add handle */
      //                        }
      //                    }
      //                }
      //            }
            
    }
        
    override def mouseReleased(e: MouseEvent) {
    }
        
    override def mouseMoved(e: MouseEvent) {
      if (isActivated) {
        if (!isAccomplished) {
          if (anchored) {
            stretchHandle(p(e))
          }
        }
        /** else, decide what kind of cursor will be used and if it's ready to be moved */
        else {
          val theHandle = getHandleAt(e.getX, e.getY)
          /** mouse points to theHandle ? */
          if (theHandle != null) {
            val idx = currHandles.indexOf(theHandle)
            if (idx >= 0) {
              selectedHandleIdx = idx
            }
                        
            cursor = HANDLE_CURSOR
          } else { /** else, mouse does not point to any handle */
            selectedHandleIdx = -1
            /** mouse points to this chart ? */
            if (chart.hits(e.getX, e.getY)) {
              setReadyToDrag(true)
              cursor = MOVE_CURSOR
            }
            /** else, mouse does not point to this chart */
            else {
              setReadyToDrag(false)
              cursor = DEFAULT_CURSOR
            }
          }
        }
      }
    }
        
    override def mouseDragged(e: MouseEvent) {
      /** only do something when isFinished() */
      if (isActivated && isAccomplished) {
        if (selectedHandleIdx != -1) {
          stretchHandle(p(e))
        } else {
          if (isReadyToDrag) {
            moveChart(p(e))
          }
        }
                
        /** notice drawingPane */
        drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this)
      }
    }
  }
    
  /**
   * Inner class of Hdl which implement Handle
   */
  protected class Hdl(apoint: ValuePoint) extends Handle {
        
    private val RADIUS = 2
        
    private val point = new ValuePoint
        
    private val bufPath = new GeneralPath
    private val bufLocation = new Point2D.Double

    if (apoint != null) {
      copyPoint(apoint)
    }
        
    def this() = this(null)

    def copyPoint(src: ValuePoint) {
      point.copy(src)
    }
        
    def getPoint: ValuePoint = {
      point
    }
        
    def getPath: GeneralPath = {
      /**
       * always replot path as not only point could have been changed,
       * but also the view's size could have been changed
       */
      plot
            
      bufPath
    }
        
    private def plot {
      val location = getLocation
            
      val x = location.getX
      val y = location.getY
            
      bufPath.reset
      bufPath.moveTo(x - RADIUS, y - RADIUS)
      bufPath.lineTo(x - RADIUS, y + RADIUS)
      bufPath.lineTo(x + RADIUS, y + RADIUS)
      bufPath.lineTo(x + RADIUS, y - RADIUS)
      bufPath.closePath
    }
        
    private def getLocation: Point2D = {
      val x = datumPlane.xb(datumPlane.bt(point.t))
      val y = datumPlane.yv(point.v)
            
      bufLocation.setLocation(x, y)
            
      bufLocation;
    }
        
    def contains(x: Int, y: Int): Boolean = {
      /**
       * always recompute location as not only point could have been changed,
       * but also the view's size could have been changed
       */
      val location = getLocation
            
      val centerx = location.getX
      val centery = location.getY
            
      if (x <= centerx + RADIUS && x >= centerx - RADIUS && y <= centery + RADIUS && y >= centery - RADIUS) {
        true
      } else {
        false
      }
    }
        
    def render(g: Graphics) {
      g.setColor(LookFeel().handleColor)
      g.asInstanceOf[Graphics2D].draw(getPath)
    }
        
    override def equals(o: Any): Boolean = {
      o match {
        case x: Handle => point.equals(x.getPoint)
        case _ => false
      }
    }
  }
    
}



