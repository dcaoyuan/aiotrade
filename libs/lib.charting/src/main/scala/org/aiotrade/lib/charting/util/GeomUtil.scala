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
package org.aiotrade.lib.charting.util

import java.awt.geom.Arc2D

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 9:36 PM
 * @since   1.0.4
 */
object GeomUtil {
    
  def hOne(vRange: Double, hRange: Double): Double = {
    (if (vRange == 0) 1.0 else hRange / vRange)
  }
    
  def yv(v: Double, hOne: Double, vMin: Double, yLower: Double): Int = {
    -((hOne * (v - vMin) - yLower)).toInt
  }
    
  def vy(y: Double, hOne: Double, vMin: Double, yLower: Double): Double = {
    -((y - yLower) / hOne - vMin)
  }
    
  def yOfLine(x: Double, baseX: Double, baseY: Double, k: Double): Double = {
    (baseY + (x - baseX) * k)
  }
    
  /**
   * @param x
   * @param xCenter center point x of arc
   * @param yCenter center point y of arc
   * @return y or Null.Double
   */
  def yOfCircle(x: Double, xCenter: Double, yCenter: Double, radius: Double, positiveSide: Boolean): Double = {
    val dx = x - xCenter
    val dy = math.sqrt(radius * radius - dx * dx)
    (if (positiveSide) yCenter + dy else yCenter - dy)
  }
    
  def yOfCircle(x: Double, circle: Arc2D, positiveSide: Boolean): Double = {
    val xCenter = circle.getCenterX
    val yCenter = circle.getCenterY
    val radius  = circle.getHeight / 2.0
    yOfCircle(x, xCenter, yCenter, radius, positiveSide)
  }
    
  def distanceToCircle(x: Double, y: Double, circle: Arc2D): Double = {
    val xCenter = circle.getCenterX
    val yCenter = circle.getCenterY
    val radius  = circle.getHeight / 2.0
    val dx = x - xCenter
    val dy = y - yCenter
    (math.sqrt(dx * dx + dy * dy) - radius)
  }
    
  def samePoint(x1: Double, y1: Double, x2: Double, y2: Double): Boolean = {
    x1.toInt == x2.toInt && y1.toInt == y2.toInt
  }
    
}
