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

import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.geom.GeneralPath
import javax.swing.JComponent
import javax.swing.Timer
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
object AbstractScrollControl {
  private val SCROLL_SPEED_THROTTLE = 60 // delay in milli seconds
  private val RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
  private val DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)

  private abstract class Position
  private object Position {
    case object OnDecreaseArrow extends Position
    case object OnIncreaseArrow extends Position
    case object OnThumb extends Position
    case object OnThumbBegSide extends Position
    case object OnThumbEndSide extends Position
    case object AfterThumb extends Position
    case object BeforeThumb extends Position
    case object DontCare extends Position
  }
}

import AbstractScrollControl._
abstract class AbstractScrollControl extends JComponent {

  private val arrowComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)
  private val thumbComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)
  private val trackComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f)
  private var readyToDragThumb, readyToDragThumbBegSide, readyToDragThumbEndSide: Boolean = _
  private var mousePressedThumbBeg, mousePressedThumbEnd: Double = _
  private var xMousePressed, yMousePressed: Int = _
  private var mousePressedAt: Position = _
  private val bufPath = new GeneralPath
  private var scalable: Boolean = _
  private var extendable: Boolean = _
  private var autoHidden: Boolean = _
  private var hidden: Boolean = _

  setOpaque(false)

  setLayout(new BorderLayout)

  private val decrArrow = new DecreaseArrow
  decrArrow.setPreferredSize(new Dimension(12, 12))
  decrArrow.setEnabled(true)
  decrArrow.setVisible(true)
  add(decrArrow, BorderLayout.WEST)

  private val incrArrow = new IncreaseArrow
  incrArrow.setPreferredSize(new Dimension(12, 12))
  incrArrow.setEnabled(true)
  incrArrow.setVisible(true)
  add(incrArrow, BorderLayout.EAST)

  private val track = new Track
  track.setPreferredSize(new Dimension(12, 12))
  track.setEnabled(true)
  track.setVisible(true)
  add(track, BorderLayout.CENTER)

  private val thumb = new Thumb

  private val scrollTimerListener = new ScrollTimerListener
  private val scrollTimer = new Timer(SCROLL_SPEED_THROTTLE, scrollTimerListener)
  scrollTimer.setInitialDelay(300)  // default InitialDelay

  private val myMouseAdapter = new MyMouseAdapter
  addMouseListener(myMouseAdapter)
  addMouseMotionListener(myMouseAdapter)
  

  override protected def paintComponent(g0: Graphics) {
    if (isHidden) {
      return
    }

    val g = g0.asInstanceOf[Graphics2D]
    setForeground(LookFeel().axisColor)
    g.setColor(getForeground)

    val w = getWidth - 1
    val h = getHeight - 1

    bufPath.reset
    /** plot upper border lines */
    bufPath.moveTo(0, 0)
    bufPath.lineTo(w, 0)

    g.draw(bufPath)
  }

  def setAutoHidden(b: Boolean) {
    if (b != this.autoHidden) {
      this.autoHidden = b

      repaint()
    }
  }

  def isAutoHidden: Boolean = {
    autoHidden
  }

  private def setHidden(b: Boolean) {
    if (b != this.hidden) {
      this.hidden = b

      if (autoHidden) {
        repaint()
      }
    }
  }

  private def isHidden: Boolean = {
    autoHidden && hidden
  }

  def setValues(valueModelRange: Double, valueShownRange: Double, valueModelEnd: Double, valueShownEnd: Double, unit: Double, nUnitsBlock: Int) {
    /**
     * @NOTICE
     * all parameters is defined in Thumb, so we need to re-set thumb's geom
     * by values that are transfered in
     */
    thumb.setGeometryBy(valueModelRange, valueShownRange, valueModelEnd, valueShownEnd, unit, nUnitsBlock)
  }

  def getUnit: Double = {
    return thumb.getUnit
  }

  def getNUnitsBlock: Int = {
    return thumb.getNUnitsBlock
  }

  def getValueShownBeg: Double = {
    return thumb.getValueShownBeg
  }

  def getValueShownEnd: Double = {
    return thumb.getValueShownEnd
  }

  def getValueShownRange: Double = {
    return thumb.getValueShownRange
  }

  def setScalable(b: Boolean) {
    this.scalable = b
  }

  def setExtendable(b: Boolean) {
    this.extendable = b
  }

  def isScalable: Boolean = {
    scalable
  }

  def isExtendable: Boolean = {
    extendable
  }

  /**
   * @NOTICE
   * Whether the mouse is acted on arrows or thumb, always do scrolling or
   * scaling via thumb
   */
  class MyMouseAdapter extends MouseAdapter with MouseMotionListener {

    override def mousePressed(e: MouseEvent) {
      if (isHidden) {
        return
      }

      xMousePressed = e.getX
      yMousePressed = e.getY

      val thumbRect = thumb.getBounds
      mousePressedThumbBeg = thumbRect.x
      mousePressedThumbEnd = thumbRect.x + thumbRect.width

      /** set all the readyToDrag to false first, will be decided late */
      readyToDragThumb = false
      readyToDragThumbBegSide = false
      readyToDragThumbEndSide = false


      getMousePressedPosition match {
        case Position.OnDecreaseArrow =>
          thumb.scrollByUnit(-1)

          scrollTimer.stop
          scrollTimerListener.setNumberWithDirection(-1)
          scrollTimerListener.setUseBlockIncrement(false)
          scrollTimerListener.startScrollTimerIfNecessary

        case Position.OnIncreaseArrow =>
          thumb.scrollByUnit(+1)

          scrollTimer.stop
          scrollTimerListener.setNumberWithDirection(+1)
          scrollTimerListener.setUseBlockIncrement(false)
          scrollTimerListener.startScrollTimerIfNecessary

        case Position.OnThumb =>
          readyToDragThumb = true

        case Position.OnThumbBegSide =>
          if (isScalable) {
            readyToDragThumbBegSide = true
          }

        case Position.OnThumbEndSide =>
          if (isScalable) {
            readyToDragThumbEndSide = true
          }


        case Position.BeforeThumb =>
          thumb.scrollByBlock(-1)

          scrollTimer.stop
          scrollTimerListener.setNumberWithDirection(-1)
          scrollTimerListener.setUseBlockIncrement(true)
          scrollTimerListener.startScrollTimerIfNecessary

        case Position.AfterThumb =>
          thumb.scrollByBlock(+1)

          scrollTimer.stop
          scrollTimerListener.setNumberWithDirection(+1)
          scrollTimerListener.setUseBlockIncrement(true)
          scrollTimerListener.startScrollTimerIfNecessary

        case _ =>
      }

    }

    override def mouseReleased(e: MouseEvent) {
      scrollTimer.stop

      readyToDragThumb = false
      readyToDragThumbBegSide = false
      readyToDragThumbEndSide = false
    }

    override def mouseDragged(e: MouseEvent) {
      if (isHidden) {
        return
      }

      if (readyToDragThumb) {

        val xMoved = e.getX - xMousePressed

        val newThumbBeg = mousePressedThumbBeg + xMoved
        thumb.drag(newThumbBeg)

      } else if (readyToDragThumbBegSide) {

        val xMoved = e.getX - xMousePressed

        val newThumbBeg = mousePressedThumbBeg + xMoved
        thumb.dragBegSide(newThumbBeg)

      } else if (readyToDragThumbEndSide) {

        val xMoved = e.getX - xMousePressed

        val newThumbEnd = mousePressedThumbEnd + xMoved
        thumb.dragEndSide(newThumbEnd)
      }

      /** don't forget to update to the new mouse pressed position */
      xMousePressed = e.getX
      yMousePressed = e.getY

      val thumbRect = thumb.getBounds
      mousePressedThumbBeg = thumbRect.x
      mousePressedThumbEnd = thumbRect.x + thumbRect.width
    }

    override def mouseMoved(e: MouseEvent) {
      if (isHidden) {
        return
      }

      if (!isScalable) {
        return
      }

      setCursor(DEFAULT_CURSOR)

      val xMouse = e.getX
      val yMouse = e.getY

      val thumbRect = thumb.getBounds
      val xThumbRectBeg = thumbRect.x
      val xThumbRectEnd = thumbRect.x + thumbRect.width

      if (xMouse > xThumbRectBeg - 1 && xMouse < xThumbRectBeg + 1) {
        /** on thumbBegSide */
        setCursor(RESIZE_CURSOR)
      } else if (xMouse > xThumbRectEnd - 1 && xMouse < xThumbRectEnd + 1) {
        /** on thumbEndSide */
        setCursor(RESIZE_CURSOR)
      }
    }

    override def mouseEntered(e: MouseEvent) {
      setHidden(false)
    }

    override def mouseExited(e: MouseEvent) {
      setHidden(true)
    }
  }

  private def getMousePressedPosition: Position = {
    if (yMousePressed < 0 || yMousePressed > getHeight()) {
      return Position.DontCare
    }

    val decrArrowRect = decrArrow.getBounds();
    if (xMousePressed > decrArrowRect.x && xMousePressed < decrArrowRect.x + decrArrowRect.width) {
      return Position.OnDecreaseArrow
    }

    val incrArrowRect = incrArrow.getBounds();
    if (xMousePressed > incrArrowRect.x && xMousePressed < incrArrowRect.x + incrArrowRect.width) {
      return Position.OnIncreaseArrow
    }

    val thumbRect = thumb.getBounds
    val xThumbRectBeg = thumbRect.x
    val xThumbRectEnd = thumbRect.x + thumbRect.width
    if (xMousePressed >= xThumbRectBeg + 1 && xMousePressed <= xThumbRectEnd - 1) {
      return Position.OnThumb
    }

    if (xMousePressed > xThumbRectBeg - 1 && xMousePressed < xThumbRectBeg + 1) {
      return Position.OnThumbBegSide
    }

    if (xMousePressed > xThumbRectEnd - 1 && xMousePressed < xThumbRectEnd + 1) {
      return Position.OnThumbEndSide
    }

    if (xMousePressed < xThumbRectBeg && xMousePressed > decrArrowRect.x + decrArrowRect.width) {
      return Position.BeforeThumb
    }

    if (xMousePressed > xThumbRectEnd && xMousePressed < incrArrowRect.x) {
      return Position.AfterThumb
    }

    Position.DontCare
  }

  private class DecreaseArrow extends JComponent {

    private val bufPath = new GeneralPath

    setOpaque(false)

    override protected def paintComponent(g0: Graphics) {
      if (isHidden) {
        return
      }

      val g = g0.asInstanceOf[Graphics2D]
      val backupComposite = g.getComposite
      g.setComposite(arrowComposite)

      setForeground(LookFeel().axisColor)
      g.setColor(getForeground)

      val w = getWidth
      val h = getHeight
      val wc = w / 2
      val hc = h / 2

      /** draw border */
      g.drawRect(0, 0, w - 1, h - 1)

      /** draw left arrow */
      bufPath.reset
      bufPath.moveTo(wc - 4, hc)
      bufPath.lineTo(wc + 3, hc - 4)
      bufPath.lineTo(wc + 3, hc + 4)
      bufPath.closePath
      g.fill(bufPath)

      if (backupComposite != null) {
        g.setComposite(backupComposite)
      }
    }
  }

  private class IncreaseArrow extends JComponent {

    private val bufPath = new GeneralPath

    setOpaque(false)

    override protected def paintComponent(g0: Graphics) {
      if (isHidden) {
        return
      }

      val g = g0.asInstanceOf[Graphics2D]
      val backupComposite = g.getComposite
      g.setComposite(arrowComposite)

      setForeground(LookFeel().axisColor)
      g.setColor(getForeground)

      val w = getWidth
      val h = getHeight
      val wc = w / 2
      val hc = h / 2

      /** draw border */
      g.drawRect(0, 0, w - 1, h - 1)

      /** draw right arrow */
      bufPath.reset
      bufPath.moveTo(wc + 4, hc)
      bufPath.lineTo(wc - 3, hc - 4)
      bufPath.lineTo(wc - 3, hc + 4)
      bufPath.closePath
      g.fill(bufPath)

      if (backupComposite != null) {
        g.setComposite(backupComposite)
      }
    }
  }

  private class Track extends JComponent {

    private val bufPath = new GeneralPath
    private val bufRect = new Rectangle

    
    setOpaque(false)

    override protected def paintComponent(g0: Graphics) {
      if (isHidden) {
        return
      }

      val w = getWidth
      val h = getHeight

      val g = g0.asInstanceOf[Graphics2D]
      val backupComposite = g.getComposite
      /** draw track */
      bufRect.setRect(0.0, 1.0, w, h - 1.0)

      setForeground(LookFeel().getTrackColor)
      g.setColor(getForeground)
      g.setComposite(trackComposite)
      g.draw(bufRect)

      if (backupComposite != null) {
        g.setComposite(backupComposite)
      }

      thumb.paintOn(g0, getBounds)
    }
  }

  /**
   * Thumb is not a JComponent, is will be painted by Track
   */
  class Thumb {

    private var trackBeg: Double = _   // the most left absolute position's x of track
    private var trackEnd: Double = _   // the most right absolute position's x of track
    private var wOne: Double = _       // width in pixels per 1.0 (pixels / value)
    private var valueModelRange: Double = _ // such as nBars of ser
    private var valueModelBeg: Double = _   // such as last position of ser items
    private var valueModelEnd: Double = _   // such as last position of ser items
    private var valueShownRange: Double = _ // such as nBars of view
    private var valueShownBeg: Double = _
    private var valueShownEnd: Double = _   // such as end position of ser shown on screen(a movable window of all items)
    private var thumbRange: Double = _
    private var thumbBeg: Double = _   // absolute position in XContenetPane
    private var thumbEnd: Double = _   // absolute position in XContenetPane
    private var unit = 1.0   // value of unit, in thumb and track, one unit means 1 pixel, in valueModel and valueShown, is value: 1.0f
    private var nUnitsBlock = 1
    private val thumbBounds = new Rectangle
    private val bufRect = new Rectangle

    /**
     * track <-> model
     * thumb <-> shown
     */
    def setGeometryBy(valueModelRange: Double, valueShownRange: Double, valueModelEnd: Double, valueShownEnd: Double, unit: Double, blockNUnits: Int) {
      this.valueModelRange = valueModelRange
      this.valueShownRange = valueShownRange
      this.valueModelEnd = valueModelEnd
      this.valueShownEnd = valueShownEnd
      this.unit = unit
      this.nUnitsBlock = blockNUnits

      this.valueModelBeg = valueModelEnd - valueModelRange
      this.valueShownBeg = valueShownEnd - valueShownRange

      val trackRect = track.getBounds()
      this.trackBeg = trackRect.x
      this.trackEnd = trackRect.x + trackRect.width

      this.wOne = (trackEnd - trackBeg) / (valueModelEnd - valueModelBeg)

      this.thumbBeg = xv(valueShownBeg)
      this.thumbEnd = xv(valueShownEnd)
      this.thumbRange = thumbEnd - thumbBeg
    }

    final private def xv(v: Double): Double = {
      trackBeg + wOne * (v - valueModelBeg)
    }

    final private def vx(x: Double): Double = {
      (x - trackBeg) / wOne + valueModelBeg
    }

    private def setThumb(thumbBeg: Double, thumbEnd: Double) {
      this.thumbBeg = thumbBeg
      this.thumbEnd = thumbEnd
      this.thumbRange = thumbEnd - thumbBeg
    }

    private def setValueShown(valueShownBeg: Double, valueShownEnd: Double) {
      this.valueShownBeg = valueShownBeg
      this.valueShownEnd = valueShownEnd
      this.valueShownRange = valueShownEnd - valueShownBeg
    }

    def getBounds: Rectangle = {
      thumbBounds.setRect(thumbBeg, 0, thumbRange, getHeight)

      thumbBounds
    }

    def getUnit: Double = {
      unit
    }

    def getNUnitsBlock: Int = {
      nUnitsBlock
    }

    def getValueShownBeg: Double = {
      valueShownBeg
    }

    def getValueShownEnd: Double = {
      valueShownEnd
    }

    def getValueShownRange: Double = {
      valueShownRange
    }

    def scrollByUnit(nUnitsWithDirection: Double) {
      val nUnitsMoved = nUnitsWithDirection
      val valueMoved = nUnitsMoved * unit
      val newValueShownBeg = valueShownBeg + valueMoved
      val newValueShownEnd = valueShownEnd + valueMoved
      val newThumbBeg = xv(newValueShownBeg)
      val newThumbEnd = xv(newValueShownEnd)

      if (!isExtendable) {
        if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
          return
        }
      }

      setValueShown(newValueShownBeg, newValueShownEnd)
      setThumb(newThumbBeg, newThumbEnd)

      viewScrolledByUnit(nUnitsMoved)
    }

    def scrollByBlock(nBlocksWithDirection: Double) {
      scrollByUnit(nBlocksWithDirection * nUnitsBlock)
    }

    def drag(newThumbBeg: Double) {
      val xMoved = newThumbBeg - thumbBeg
      val newThumbEnd = newThumbBeg + thumbRange
      val newValueShownBeg = vx(newThumbBeg)
      val newValueShownEnd = vx(newThumbEnd)
      val valueMoved = newValueShownBeg - valueShownBeg
      val nUnitsMoved = valueMoved / unit

      if (!isExtendable) {
        if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
          return
        }
      }

      setThumb(newThumbBeg, newThumbEnd)
      setValueShown(newValueShownBeg, newValueShownEnd)

      viewScrolledByUnit(nUnitsMoved)
    }

    def dragBegSide(anewThumbBeg: Double) {
      var newThumbBeg = anewThumbBeg
      var xMoved = newThumbBeg - thumbBeg
      var newThumbEnd = thumbEnd // keeps the same what ever
      var newThumbRange = newThumbEnd - newThumbBeg
      if (newThumbRange < 4) {
        /** thumbRange should be at lease size of: */
        newThumbRange = 4
        xMoved = thumbRange - newThumbRange
        newThumbBeg = thumbBeg + xMoved
        newThumbEnd = newThumbBeg + newThumbRange
      }
      val newValueShownBeg = vx(newThumbBeg)
      val newValueShownEnd = vx(newThumbEnd)

      if (!isExtendable) {
        if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
          return;
        }
      }

      setThumb(newThumbBeg, newThumbEnd)
      setValueShown(newValueShownBeg, newValueShownEnd)

      viewScaledToRange(valueShownRange)
    }

    def dragEndSide(anewThumbEnd: Double) {
      var newThumbEnd = anewThumbEnd
      var xMoved = newThumbEnd - thumbEnd
      var newThumbBeg = thumbBeg; // keeps the same what ever
      var newThumbRange = newThumbEnd - newThumbBeg
      if (newThumbRange < 4) {
        /** thumbRange should be at lease size of: */
        newThumbRange = 4
        xMoved = newThumbRange - thumbRange
        newThumbEnd = thumbEnd + xMoved
        newThumbBeg = newThumbEnd - newThumbRange
      }
      val newValueShownBeg = vx(newThumbBeg)
      val newValueShownEnd = vx(newThumbEnd)
      val valueMoved = newValueShownEnd - valueShownEnd
      val nUnitsMoved = valueMoved / unit

      if (!isExtendable) {
        if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
          return
        }
      }

      setThumb(newThumbBeg, newThumbEnd)
      setValueShown(newValueShownBeg, newValueShownEnd)

      viewScrolledByUnit(nUnitsMoved)

      viewScaledToRange(valueShownRange)
    }

    def paintOn(g0: Graphics, containerAbsoluteRect: Rectangle) {
      /**
       * !NOTICE
       * thumb.getBounds() is relative to XContentPane, not Track, so,
       * should recompute the bounds relative to containerAbsoluteRect
       */
      val thumbAbsoluteRect = thumb.getBounds
      bufRect.setRect(thumbAbsoluteRect.x - containerAbsoluteRect.x,
                      thumbAbsoluteRect.y,
                      thumbAbsoluteRect.width,
                      thumbAbsoluteRect.height)

      val g = g0.asInstanceOf[Graphics2D]
      val backupComposite = g.getComposite();
      g.setColor(LookFeel().getThumbColor)

      /** draw thumb */
      g.setComposite(thumbComposite)
      g.fill(bufRect)

      /** draw thumb sides */
      g.setComposite(arrowComposite)
      g.draw(bufRect)

      if (backupComposite != null) {
        g.setComposite(backupComposite)
      }
    }
  }

  /**
   * Listener for timer events.
   */
  class ScrollTimerListener(nUnitsWithDirection: Double, private var useBlockIncrement: Boolean, default: Boolean) extends ActionListener {

    private var numberWithDirection = +1.0

    if (default) {
      this.numberWithDirection = +1
    } else {
      this.numberWithDirection = nUnitsWithDirection
    }


    def this() = this(0, false, true)

    def this(nUnitsWithDirection: Double, useBlockIncrement: Boolean) = this(nUnitsWithDirection, useBlockIncrement, false)

    def setNumberWithDirection(nUnitsWithDirection: Double) {
      this.numberWithDirection = nUnitsWithDirection
    }

    def setUseBlockIncrement(useBlockIncrement: Boolean) {
      this.useBlockIncrement = useBlockIncrement;
    }

    def startScrollTimerIfNecessary {
      if (scrollTimer.isRunning) {
        return
      }

      scrollTimer.start
    }

    def actionPerformed(e: ActionEvent) {
      if (useBlockIncrement) {
        thumb.scrollByBlock(numberWithDirection)

        /** Stop scrolling if the thumb catches up with the mouse */
        val mousePressedPosition = getMousePressedPosition
        if ((numberWithDirection > 0 && mousePressedPosition != Position.AfterThumb) ||
            (numberWithDirection < 0 && mousePressedPosition != Position.BeforeThumb)) {

          e.getSource.asInstanceOf[Timer].stop
        }
      } else {
        thumb.scrollByUnit(numberWithDirection)
      }
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    if (myMouseAdapter != null) {
      removeMouseListener(myMouseAdapter)
      removeMouseMotionListener(myMouseAdapter)
    }

    super.finalize
  }

  /**
   * @param nUnitsWithdirection, the number of units that scrolled with positive/negative
   *        diretion, it may not be integer, because the dragging action may
   *        scroll fractional units
   * @TODO The following abstract method can be easily re-written to event/listener
   * -------------------------------------------------------------------
   */
  protected def viewScrolledByUnit(nUnitsWithdirection: Double): Unit

  protected def viewScaledToRange(valueShownRange: Double): Unit
}



