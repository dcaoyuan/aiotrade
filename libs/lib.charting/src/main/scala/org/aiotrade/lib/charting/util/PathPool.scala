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

import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import org.aiotrade.lib.util.pool.PoolableObjectFactory
import org.aiotrade.lib.util.pool.StackObjectPool

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, 11/27/2006
 * @since   1.0.4
 */
class PathPool(maxIdle: Int,
               initIdleCapacity: Int,
               initialCapacityOfPath: Int
) extends StackObjectPool[GeneralPath](maxIdle, initIdleCapacity) with PoolableObjectFactory[GeneralPath] {

  factory_=(this)

  @throws(classOf[RuntimeException])
  final def activate(obj: GeneralPath) {
    obj.reset
  }

  @throws(classOf[RuntimeException])
  final def destroy(obj: GeneralPath) {}

  @throws(classOf[RuntimeException])
  final def create: GeneralPath = new GeneralPath(PathIterator.WIND_NON_ZERO, initialCapacityOfPath)

  @throws(classOf[RuntimeException])
  final def passivate(obj: GeneralPath) {}
    
  final def validate(obj: GeneralPath): Boolean = true
}
