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
package org.aiotrade.lib.util.pool

/**
 * A simple base impementation of {@link ObjectPool}.
 * All optional operations are implemented as throwing
 * {@link UnsupportedOperationException}.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 383290 $ $Date: 2006-03-05 02:00:15 -0500 (Sun, 05 Mar 2006) $
 */
abstract class BaseObjectPool[T] extends ObjectPool[T] {
  
  @throws(classOf[RuntimeException])
  def borrow: T

  @throws(classOf[RuntimeException])
  def returnIt(obj: T)
  
  @throws(classOf[RuntimeException])
  def invalidate(obj: T)

  /**
   * Not supported in this base implementation.
   */
  @throws(classOf[UnsupportedOperationException])
  def numOfIdle: Int = {
    throw new UnsupportedOperationException
  }

  /**
   * Not supported in this base implementation.
   */
  @throws(classOf[UnsupportedOperationException])
  def numOfActive: Int = {
    throw new UnsupportedOperationException
  }

  /**
   * Not supported in this base implementation.
   */
  @throws(classOf[UnsupportedOperationException])
  @throws(classOf[Exception])
  def clear {
    throw new UnsupportedOperationException
  }

  /**
   * Not supported in this base implementation.
   */
  @throws(classOf[UnsupportedOperationException])
  @throws(classOf[RuntimeException])
  def add {
    throw new UnsupportedOperationException
  }

  @throws(classOf[Exception])
  def close {
    assertOpen
    closed = true
  }

  /**
   * Not supported in this base implementation.
   */
  @throws(classOf[UnsupportedOperationException])
  @throws(classOf[IllegalStateException])
  def factory_=(factory: PoolableObjectFactory[T]) {
    throw new UnsupportedOperationException();
  }
    
  protected final def isClosed: Boolean = {
    closed
  }
    
  @throws(classOf[IllegalStateException])
  protected final def assertOpen {
    if (isClosed) {
      throw new IllegalStateException("Pool not open")
    }
  }

  @volatile
  private var closed = false
}

