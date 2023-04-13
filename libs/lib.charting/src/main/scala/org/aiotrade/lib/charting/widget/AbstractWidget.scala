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

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import javax.swing.Action
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.charting.util.PathPool
import scala.collection.mutable

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:34 AM
 * @since   1.0.4
 */
object AbstractWidget {
  protected val pathPool = new PathPool(10, 10, 1000)
  protected val HIT_TEST_SQUARE_RADIUS = 2
}

import AbstractWidget._
abstract class AbstractWidget extends Widget {
    
  protected def borrowPath: GeneralPath = {
    pathPool.borrow
  }
    
  protected def returnPath(path: GeneralPath) {
    pathPool.returnIt(path)
  }
    
  protected def returnBorrowedPaths(paths: java.util.Collection[GeneralPath]) {
    val itr = paths.iterator
    while (itr.hasNext) {
      pathPool.returnIt(itr.next)
    }
  }
    
  private var _isOpaque: Boolean = _
  private var _isFilled = false
  private var _foreground: Color = Color.WHITE
  private var _background: Paint = _
    
  private var _location: Point = _
  private var _bounds: Rectangle = _
    
  private var _model: M = _
    
  private var _children: ArrayList[Widget] = _
  private var _actions: ArrayList[Action] = _
  private var colorToPathPair: mutable.Map[Color, (GeneralPath, GeneralPath)] = _
    
  def children = _children

  def isOpaque: Boolean = _isOpaque
  def setOpaque(opaque: Boolean) {
    this._isOpaque = opaque
  }

  def isFilled = _isFilled
  def isFilled_=(isFilled: Boolean) {
    this._isFilled = isFilled
  }
    
  def getBackground: Paint = _background
  def setBackground(paint:Paint) {
    this._background = paint
  }
    
  def getForeground : Color = _foreground
  def setForeground(color: Color) {
    this._foreground = color
  }
    
  def getLocation: Point = {
    if (_location == null) new Point(0, 0) else new Point(_location)
  }

  def setLocation(point: Point) {
    setLocation(point.x, point.y)
  }
    
  def setLocation(x: Double, y: Double) {
    if (_location == null) {
      _location = new Point(x.toInt, y.toInt)
    } else {
      _location.setLocation(x, y)
    }
  }
    
  def getBounds: Rectangle = {
    if (_bounds == null) makePreferredBounds else _bounds
  }

  def setBounds(rect:Rectangle) {
    setBounds(rect.x, rect.y, rect.width, rect.height)
  }
    
  def setBounds(x: Double, y: Double, width: Double, height: Double) {
    if (_bounds == null) {
      _bounds = new Rectangle(x.toInt, y.toInt, width.toInt, height.toInt)
    } else {
      _bounds.setRect(x, y, width, height)
    }
  }
    
  protected def makePreferredBounds: Rectangle = {
    val childrenBounds = new Rectangle
    if (_children != null) {
      for (child <- _children) {
        childrenBounds.add(child.getBounds)
      }
    }
        
    childrenBounds
        
    /** @TODO */
  }
    
  def contains(point: Point): Boolean = {
    contains(point.x, point.y)
  }
    
  def contains(x: Double, y: Double): Boolean = {
    contains(x, y, 1, 1)
  }
    
  def contains(rect: Rectangle): Boolean = {
    contains(rect.x, rect.y, rect.width, rect.height)
  }
    
  def contains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (isOpaque) {
      getBounds.intersects(x, y, width, height)
    } else {
      if (isContainerOnly) {
        childrenContain(x, y, width, height)
      } else {
        widgetContains(x, y, width, height) || childrenIntersect(x, y, width, height)
      }
    }
  }
    
  protected def widgetContains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    getBounds.contains(x, y, width, height);
  }
    
  protected def childrenContain(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (_children == null) return false

    _children exists (_.contains(x, y, width, height))
  }
    
  def intersects(rect: Rectangle): Boolean = {
    intersects(rect.x, rect.y, rect.width, rect.height)
  }
    
  def intersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (isOpaque) {
      getBounds.intersects(x, y, width, height)
    } else {
      if (isContainerOnly) {
        childrenIntersect(x, y, width, height)
      } else {
        widgetIntersects(x, y, width, height) || childrenIntersect(x, y, width, height)
      }
    }
  }
    
  protected def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean
    
  protected def childrenIntersect(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (_children == null) return false

    _children exists (_.intersects(x, y, width, height))
  }
    
  def hits(point: Point): Boolean = {
    hits(point.x , point.y)
  }
    
  def hits(x: Double, y: Double): Boolean = {
    intersects(
      x - HIT_TEST_SQUARE_RADIUS, y - HIT_TEST_SQUARE_RADIUS,
      2 * HIT_TEST_SQUARE_RADIUS, 2 * HIT_TEST_SQUARE_RADIUS)
  }
    
  def model: M = {
    if (_model == null) {
      _model = createModel
    }
        
    _model
  }
    
  protected def createModel: M
    
  def plot {
    reset
    plotWidget
  }
    
  protected def plotWidget
    
  def render(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]
        
    val location = getLocation
    val backupTransform = g.getTransform
    if (!(location.x == 0 && location.y == 0)) {
      g.translate(location.x, location.y)
    }
        
    val bounds = getBounds
    val backupClip = g.getClip
    g.clip(bounds)

    val clipBounds = g.getClipBounds
    if (intersects(clipBounds) || clipBounds.contains(bounds) || bounds.height == 1 || bounds.width == 1) {
      if (isOpaque) {
        g.setPaint(getBackground)
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
      }
            
      renderWidget(g0)
      renderChildren(g0)
    }
        
    g.setClip(backupClip)
    g.setTransform(backupTransform)
  }
    
  protected def renderWidget(g0: Graphics)
    
  protected def renderChildren(g0: Graphics) {
    if (_children == null) return
        
    val g = g0.asInstanceOf[Graphics2D]
        
    val clipBounds = g.getClipBounds
    for (child <- _children) {
      if (child.intersects(clipBounds) || clipBounds.contains(child.getBounds) || child.getBounds.height == 1 || child.getBounds.width == 1) {
        child match {
          case x: PathWidget =>
            if (colorToPathPair == null) colorToPathPair = mutable.Map[Color, (GeneralPath, GeneralPath)]()

            val color = child.getForeground
            val (pathToDraw, pathToFill) = colorToPathPair.get(color) getOrElse {
              val toDraw = borrowPath
              val toFill = borrowPath
              colorToPathPair.put(color, (toDraw, toFill))
              (toDraw, toFill)
            }

            val path = x.getPath                
            val location = child.getLocation
            val shape = if (location.x == 0 && location.y == 0) {
              val transform = AffineTransform.getTranslateInstance(location.x, location.y)
              path.createTransformedShape(transform)
            } else path

            if (x.isFilled) {
              pathToFill.append(shape, false)
            } else {
              pathToDraw.append(shape, false)
            }
          case _ => child.render(g)
        }
      }
    }

    if (colorToPathPair != null) {
      for ((color, (pathToDraw, pathToFill)) <- colorToPathPair) {
        g.setColor(color)
        
        if (pathToDraw != null) {
          g.draw(pathToDraw)
          returnPath(pathToDraw)
        }
        
        if (pathToFill != null) {
          g.draw(pathToFill) // g.fill only fills shape's interior, we need draw shape too
          g.fill(pathToFill) 
          returnPath(pathToFill)
        }
      }
      
      colorToPathPair.clear
    }
  }
    
  /** override it if only contains children (plotWidget() do noting) */
  def isContainerOnly: Boolean = {
    false
  }
    
  def addChild[T <: Widget](child: T): T = {
    if (_children == null) _children = new ArrayList[Widget]
    
    _children += child
    child
  }
    
  def removeChild(child: Widget) {
    if (_children == null) return

    _children.remove(_children.indexOf(child))
  }
    
  def getChildren: Seq[Widget] = {
    if (_children == null) return Nil
    
    _children
  }
    
  def resetChildren {
    if (_children == null) return
    
    for (child <- _children) {
      child.reset
    }
  }
    
  def clearChildren {
    if (_children == null) return
    
    _children.clear
  }
    
  def lookupChildren[T <: Widget](widgetType: Class[T], foreground: Color): Seq[T] = {
    if (_children == null) return Nil

    val result = new ArrayList[Widget]

    for (child <- _children) {
      if (widgetType.isInstance(child) && child.getForeground.equals(foreground)) {
        result += child
      }
    }
    
    result.asInstanceOf[Seq[T]]
  }
    
  def lookupFirstChild[T <: Widget](widgetType: Class[T], foreground: Color): Option[T] = {
    if (_children == null) return None

    _children.find{x => widgetType.isInstance(x) && x.getForeground.equals(foreground)}.asInstanceOf[Option[T]]
  }
    
  def addAction(action: Action): Action = {
    if (_actions == null) _actions = new ArrayList[Action]
        
    _actions += action
    action
  }
    
  def removeAction(action: Action) {
    if (_actions == null) return

    _actions -= action
  }

  def lookupAction[T <: Action](tpe: Class[T]): Option[T] = {
    if (_actions == null) return None

    _actions.find(tpe.isInstance(_)).asInstanceOf[Option[T]]
  }
    
  def lookupActionAt[T <: Action](tpe: Class[T], point: Point): Option[T] = {
    if (_children == null) return None
    
    /** lookup children first */
    _children find (_.contains(point)) match {
      case Some(x) => x.lookupAction(tpe)
      case None =>
        /** then lookup this */
        if (getBounds.contains(point)) lookupAction(tpe) else None
    }
  }
    
  def reset {
    if (_children == null) return
    
    for (child <- _children) {
      child.reset
    }
    _children.clear
  }

}
