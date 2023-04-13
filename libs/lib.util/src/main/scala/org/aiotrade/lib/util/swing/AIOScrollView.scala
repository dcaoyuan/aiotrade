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
package org.aiotrade.lib.util.swing

import java.awt.Graphics
import javax.swing.JComponent
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
class AIOScrollView($viewPort: JComponent, $pictures: List[JComponent]) extends JComponent {
  private val W_INTERSPACE = 10
  private val H_INTERSPACE = 10
  private val MAX_H_PER_PICTURE = 180
    
  private var viewPort = $viewPort
  private val pictures = ArrayBuffer[JComponent]() ++= $pictures
    
  private var dModelBegByRewind: Int = _
    
  private var hViewPort: Int = _
  private var wViewPort: Int = _
  private var nPictures: Int = _
  private var wPicture: Int = _
  private var hPicture: Int = _
  private var nRows: Int = _
  private var wRow: Int = _
  private var hRow: Int = _
    
  private var shouldAlign: Boolean = _
    
  private var shownRange: Int = _
  private var modelBeg: Int = _
  private var modelRange: Int = _
  private var modelBegBeforeRewind: Int = _
    
  private var frozen: Boolean = false
    
  /** <idx, x> pair */
  private val idxToX = mutable.Map[Int, Int]()

  setOpaque(true)
  setDoubleBuffered(true)
    
  /**
   * Let's image the scrolling is the model scrolling from right to left
   * and shown keeps the same. the axis origin is the scrollView's origin.
   * so, the shownBeg is always 0, and shownEnd is always shownRange
   */
  def scrollByPixel(nPixels: Int): Unit = {
    if (frozen) {
      return
    }
        
    modelBeg -= nPixels
        
    repaint()
  }
    
  def scrollByPicture(nPictures: Int): Unit = {
    if (frozen) {
      return
    }
        
    shouldAlign = true
        
    scrollByPixel(wRow * nPictures)
  }
    
  def freeze: Unit = {
    this.frozen = true
  }
    
  def unFreeze: Unit = {
    this.frozen = false
  }
    
  def isFrozen: Boolean = {
    frozen
  }

  def addPicture(picture: JComponent) {
    pictures += picture
  }

  def removePicture(picture: JComponent) {
    pictures -= picture
  }

  override def paint(g: Graphics) {
    hViewPort = viewPort.getHeight
    wViewPort = viewPort.getWidth
        
    hPicture = if (hViewPort > MAX_H_PER_PICTURE) MAX_H_PER_PICTURE else hViewPort
    wPicture = (hPicture * 1.382).toInt
        
    hRow = hPicture + W_INTERSPACE
    wRow = wPicture + H_INTERSPACE
    nRows = hViewPort / hRow
    if (nRows <= 0) {
      nRows = 1
    }
        
    /**
     * keep my bounds' width same as viewPort's, but, my height could be greater
     * than viewPort's height
     */
    var hBoundsMe = hRow * nRows
    if (hBoundsMe < hViewPort) {
      hBoundsMe = hViewPort
    }
    setBounds(0, 0, wViewPort, hBoundsMe)
    g.setColor(getBackground)
    g.fillRect(0, 0, wViewPort, hBoundsMe)
        
    /** image that the shownRange is jointed by multiple rows */
    shownRange = wViewPort * nRows
        
    nPictures = pictures.size
    /** add one more picture space for scrolling */
    modelRange = wRow * (nPictures + 1)
    if (modelRange < shownRange) {
      modelRange = shownRange
    }
        
    rewindIfNecessary
        
    computePicturesPosition
        
    alignIfNecessary
        
    placePictures
        
    super.paint(g)
  }
    
  private def rewindIfNecessary {
    /** check if need rewind scrolling */
    val modelEnd = modelBeg + modelRange
    val diff = modelEnd - shownRange
    if (diff < 0) {
      /** rewind happens */
      val old = modelBeg;
      modelBeg = shownRange - diff
            
      dModelBegByRewind = modelBeg - old
    }
  }
    
  private def computePicturesPosition {
    idxToX.clear
    for (i <- 0 until pictures.size) {
      var x0 = modelBeg + i * wRow
      val x0BeforeRewound = x0 - dModelBegByRewind
            
      val x1BeforeRewound = x0BeforeRewound + wRow
      if (x1BeforeRewound > 0 && x1BeforeRewound < modelBeg) {
        /** before rewound, it's still in view rande, so let it be shown */
        x0 = x0BeforeRewound
      }
            
      idxToX.put(i, x0)
    }
  }
    
  private def alignIfNecessary {
    var pixelsScrollBack = 0
        
    var noneInFront = true
    if (shouldAlign) {
      /** find  the current front picture, align it at 0 */
      for ((idx0, x0) <- idxToX find {case (idx, x) => x < 0 && x + wRow > 0}) {
        pixelsScrollBack = -x0
        noneInFront = false
      }
      
      if (noneInFront) {
        /*-
         * now is showing a width > wRow space, the idx0 one will be the next
         * Do we enjoy the space for a rest? if not, just do:
         * if (xMap.size() > 0) {
         *     pixelsScrollBack = xMap.get(0)
         * }
         */
      }
            
      if (pixelsScrollBack > 0) {
        modelBeg += pixelsScrollBack
                
        rewindIfNecessary
        computePicturesPosition
      }
            
      /** shouldAlign reset */
      shouldAlign = false
    }
  }
    
  private def placePictures {
    for ((idx, x0) <- idxToX) {
      val picture = pictures(idx)
            
      if (x0 + wPicture < 0 || x0 > shownRange) {
        /** not in the scope of viewRange */
        picture.setVisible(false)
      } else {
        val rowIdx = x0 / wViewPort
                
        /** adjust to this row's x and y */
        val xInRow = x0 - wViewPort * rowIdx
        val yInRow = hRow * rowIdx
                
        picture.setBounds(xInRow, yInRow, wPicture, hPicture)
        picture.setVisible(true)
      }
    }
  }
    
}

