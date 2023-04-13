/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities.model

trait Flag {
  import Flag._
  
  /** dafault could be set to 1, which is closed_! */
  def flag: Int 
  def flag_=(flag: Int)

  def closed_? : Boolean = (flag & MaskClosed) == MaskClosed
  def closed_!   {flag |=  MaskClosed}
  def unclosed_! {flag &= ~MaskClosed}

  def justOpen_? : Boolean = (flag & MaskJustOpen) == MaskJustOpen
  def justOpen_!   {flag |=  MaskJustOpen}
  def unjustOpen_! {flag &= ~MaskJustOpen}

  /** is this value created/composed by me or loaded from remote or other source */
  def fromMe_? : Boolean = (flag & MaskFromMe) == MaskFromMe
  def fromMe_!   {flag |=  MaskFromMe}
  def unfromMe_! {flag &= ~MaskFromMe}
  
}

object Flag {
  // bit masks for flag
  val MaskClosed    = 1 << 0   // 1   2^^0    000...00000001
  val MaskVerified  = 1 << 1   // 2   2^^1    000...00000010
  val MaskFromMe    = 1 << 2   // 4   2^^2    000...00000100
  val flagbit4      = 1 << 3   // 8   2^^3    000...00001000
  val flagbit5      = 1 << 4   // 16  2^^4    000...00010000
  val flagbit6      = 1 << 5   // 32  2^^5    000...00100000
  val flagbit7      = 1 << 6   // 64  2^^6    000...01000000
  val MaskJustOpen  = 1 << 7   // 128 2^^7    000...10000000
}

