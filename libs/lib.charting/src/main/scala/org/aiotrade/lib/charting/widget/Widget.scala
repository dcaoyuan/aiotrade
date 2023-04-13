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
import java.awt.Paint
import java.awt.Point
import java.awt.Rectangle
import javax.swing.Action

/**
 * bounds(x, y, width, height) is relative to location(lx, ly), so the final
 * position of widget will be shifted by offset: location(lx, ly upon to
 * (bounds.x, bounds.y). That is, the coordinate (x, y) of bounds' left-top
 * corner relative to origin point(0, 0) will be (x + lx, y + ly).
 *
 * We can use the different bounds + offset(location) combination to define the
 * postion of widget and move it.
 *
 * origin(0,0)
 * +------------------------------------------------------> x
 * |
 * |    * location(lx, ly)
 * |     \
 * |      \
 * |       \ (x + lx, y + ly)
 * |        +------------+  -
 * |        |            |  |
 * |        |   bounds   | height
 * |        |            |  |
 * |        +------------+  _
 * |        |--  width --|
 * |
 * |
 * V
 * y
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:21 AM
 * @since   1.0.4
 */
trait Widget {

  type M >: Null <: WidgetModel

  def setOpaque(opaque: Boolean)
  def isOpaque: Boolean

  def isFilled: Boolean
  def isFilled_=(isFilled: Boolean)
    
  def setBackground(paint: Paint)
  def getBackground: Paint
    
  def setForeground(color: Color)
  def getForeground: Color
    
  def setLocation(location: Point)
  def setLocation(x: Double, y: Double)
  def getLocation: Point
    
  def setBounds(rect: Rectangle)
  def setBounds(x: Double, y: Double, width: Double, height: Double)
  def getBounds: Rectangle
    
  def contains(point: Point): Boolean
  def contains(x: Double, y: Double): Boolean
  def contains(rect: Rectangle): Boolean
  def contains(x: Double, y: Double, width: Double, height: Double): Boolean
  def intersects(rect: Rectangle): Boolean
  def intersects(x: Double, y: Double, width: Double, height: Double): Boolean
  def hits(point: Point): Boolean
  def hits(x: Double, y: Double): Boolean
    
  def model: M
    
  def plot
  def render(g: Graphics)
  def reset

  def isContainerOnly: Boolean
    
  def addChild[T <: Widget](child: T): T
  def removeChild(child: Widget)
  def children: Seq[Widget]
  def clearChildren
  def lookupChildren[T <: Widget](widgetType: Class[T], foreground: Color): Seq[T]
  def lookupFirstChild[T <: Widget](widgetType: Class[T], foreground: Color): Option[T]
    
  def addAction(action: Action): Action
  def removeAction(action: Action)
  def lookupAction[T <: Action](tpe: Class[T]): Option[T]
  def lookupActionAt[T <: Action](tpe: Class[T], point: Point): Option[T]

}
