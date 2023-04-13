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
package org.aiotrade.lib.charting.laf

import java.awt.Color;
import java.awt.Font;

/**
 *
 * @author Caoyuan Deng
 */
class Gray extends LookFeel {

  val monthColors = Array(
    Color.cyan.darker.darker.darker,
    Color.cyan.darker.darker,
    Color.yellow.darker.darker.darker,
    Color.red.darker.darker.darker,
    Color.red.darker.darker,
    Color.yellow.darker.darker.darker,
    Color.white.darker.darker.darker,
    Color.white.darker.darker,
    Color.yellow.darker.darker.darker,
    Color.magenta.darker.darker.darker,
    Color.magenta.darker.darker,
    Color.yellow.darker.darker.darker
  )

  val planetColors = Array(
    Color.magenta.darker,
    Color.white,
    Color.blue,
    Color.red,
    Color.cyan,
    Color.yellow,
    Color.orange.darker.darker,
    Color.green.darker.darker,
    Color.gray.darker.darker,
    Color.blue
  )

  val axisFont = new Font("Dialog Input", Font.PLAIN, 9)

  val chartColors = Array(
    Color.PINK.darker, // Sun
    Color.BLUE.darker, // Mercury
    Color.BLACK, // Venus
    Color.GREEN.darker, // Earth
    Color.RED.darker, // Mars
    Color.CYAN.darker, // Jupiter
    Color.YELLOW.darker.darker, // Saturn
    Color.PINK.darker, // Uranus
    Color.LIGHT_GRAY, // Neptune
    Color.MAGENTA.darker, // Pluto
    Color.LIGHT_GRAY // MOON
    //            Color.BLUE.darker,
    //                    Color.RED.darker,
    //                    Color.GREEN.darker,
    //                    Color.CYAN.darker,
    //                    Color.MAGENTA.darker,
    //                    Color.PINK.darker,
    //                    Color.ORANGE.darker,
    //                    Color.YELLOW.darker,
    //                    Color.BLACK,
  )


  val systemBackgroundColor = new Color(212, 208, 200)

  val nameColor = Color.BLACK

  val backgroundColor = systemBackgroundColor
  val infoBackgroundColor = backgroundColor
  val heavyBackgroundColor = backgroundColor

  val axisBackgroundColor = systemBackgroundColor

  val stickChartColor = Color.BLACK

  val positiveColor = Color.BLACK
  val negativeColor = Color.BLACK

  val positiveBgColor = Color.GREEN
  val negativeBgColor = Color.RED
  val neutralBgColor = neutralColor

  val borderColor = Color.BLACK

  /** same as new Color(0.0f, 0.0f, 1.0f, 0.382f) */
  val referCursorColor = new Color(0.0f, 0.5f, 0.5f, 0.618f) //new Color(0.5f, 0.0f, 0.5f, 0.618f); //new Color(0.0f, 0.0f, 1.0f, 0.618f);
  //new Color(131, 129, 221);

  /** same as new Color(1.0f, 1.0f, 1.0f, 0.618f) */
  val mouseCursorColor = new Color(1.0f, 1.0f, 1.0f, 0.618f)
  //new Color(239, 237, 234);

  val mouseCursorTextColor = Color.BLACK
  val mouseCursorTextBgColor = Color.YELLOW
  val referCursorTextColor = Color.WHITE
  val referCursorTextBgColor = referCursorColor.darker

  val drawingMasterColor = Color.BLUE // new Color(128, 128, 255); //new Color(128, 0, 128);
  val drawingColor = Color.BLUE.darker // new Color(128, 128, 255); //new Color(128, 0, 128);
  val drawingColorTransparent = new Color(0.0f, 0.0f, 1.f, 0.382f) //new Color(128, 0, 128);
  val handleColor = Color.YELLOW // new Color(128, 128, 255); //new Color(128, 0, 128);

  val astrologyColor = Color.BLACK

  override val axisColor = Color.BLACK
  override val trackColor = Color.WHITE
  override val thumbColor = Color.BLACK

}

